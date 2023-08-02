package com.github.novicezk.midjourney.aliyun;

import com.aliyun.oss.common.utils.DateUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class StsService {
    // regionId表示RAM的地域ID。以华东1（杭州）地域为例，regionID填写为cn-hangzhou。也可以保留默认值，默认值为空字符串（""）。
    private final String regionId = "cn-hangzhou";
    private final String product = "Sts";
    // STS接入地址，例如sts.cn-hangzhou.aliyuncs.com。
    private final String endpoint = "sts.cn-hangzhou.aliyuncs.com";
    // 填写步骤1生成的RAM用户访问密钥AccessKey ID和AccessKey Secret。
    private final String accessKeyId = "";
    private final String accessKeySecret = "";
    // 填写步骤3获取的角色ARN。
    private final String roleArn = "";
    // 自定义角色会话名称，用来区分不同的令牌，例如可填写为SessionTest。
    private final String roleSessionName = "threeing";
    // 设置临时访问凭证的有效时间为3600秒，1小时，临时Token最多只能设置1H
    Long durationSeconds = 3600L;
    // 如果policy为空，则用户将获得该角色下所有权限。
    private final String POLICY = "{\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Action\": [\n" +
            "        \"oss:PutObject\"\n" +
            "      ],\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Resource\": [\"acs:oss:*:*:*\"]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Version\": \"1\"\n" +
            "}\n";



    //Oss 临时访问的ak、as、token
    private OssToken token = new OssToken();
    private Date ossExpiration = null;


    public StsService(){//初始化临时Token等值
        initToken();
    }


    public OssToken getOssToken() {
        checkOssExpriation();
        return this.token;
    }

    private void checkOssExpriation(){//检查token是否快过期
        if((this.ossExpiration == null)){
            initToken();
        }

        Date current = new Date();
        if((this.ossExpiration.getTime() - current.getTime())/1000<600){//小于10分钟内重新获取token
            initToken();
        }
    }

    private void initToken() {
        try {
            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(regionId, product, endpoint);
            // 添加endpoint。适用于Java SDK 3.12.0以下版本。
            // DefaultProfile.addEndpoint("",regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            // 适用于Java SDK 3.12.0及以上版本。
            request.setSysMethod(MethodType.POST);
            // 适用于Java SDK 3.12.0以下版本。
            //request.setMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);
            request.setPolicy(POLICY);
            request.setDurationSeconds(durationSeconds);
            final AssumeRoleResponse response = client.getAcsResponse(request);
            this.ossExpiration = DateUtil.parseIso8601Date(response.getCredentials().getExpiration());
            this.token.setAccessKeyId(response.getCredentials().getAccessKeyId());
            this.token.setAccessKeySecret(response.getCredentials().getAccessKeySecret());
            this.token.setToken(response.getCredentials().getSecurityToken());
            //System.out.println("RequestId: " + response.getRequestId());
        } catch (ClientException e) {
            log.error("获取oss token失败, code: {}, msg: {}, requestId:{}", e.getErrCode(), e.getErrMsg(),e.getRequestId());
        }catch (Exception e) {
            log.error("转换oss过期日期失败, msg:", e);
        }
    }


    public static void main(String[] args) {
        // STS接入地址，例如sts.cn-hangzhou.aliyuncs.com。
        String endpoint = "sts.cn-hangzhou.aliyuncs.com";
        // 填写步骤1生成的RAM用户访问密钥AccessKey ID和AccessKey Secret。
        String accessKeyId = "LTAI5tQtMCjLVWbnNGPDSSkP";
        String accessKeySecret = "MKDtu2RoX4QRvRAsnz2LB92iP0aDnC";
        // 填写步骤3获取的角色ARN。
        String roleArn = "acs:ram::31426079:role/ramosstest";
        // 自定义角色会话名称，用来区分不同的令牌，例如可填写为SessionTest。
        String roleSessionName = "yourRoleSessionName";
        // 以下Policy用于限制仅允许使用临时访问凭证向目标存储空间examplebucket下的src目录上传文件。
        // 临时访问凭证最后获得的权限是步骤4设置的角色权限和该Policy设置权限的交集，即仅允许将文件上传至目标存储空间examplebucket下的src目录。
        String POLICY = "{\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Action\": [\n" +
                "        \"oss:PutObject\"\n" +
                "      ],\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Resource\": [\"acs:oss:*:*:*\"]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"Version\": \"1\"\n" +
                "}\n";
        // 设置临时访问凭证的有效时间为3600秒。
        Long durationSeconds = 3600L;
        try {
            // regionId表示RAM的地域ID。以华东1（杭州）地域为例，regionID填写为cn-hangzhou。也可以保留默认值，默认值为空字符串（""）。
            String regionId = "cn-hangzhou";
            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(regionId, "Sts", endpoint);
            // 添加endpoint。适用于Java SDK 3.12.0以下版本。
            // DefaultProfile.addEndpoint("",regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            // 适用于Java SDK 3.12.0及以上版本。
            request.setSysMethod(MethodType.POST);
            // 适用于Java SDK 3.12.0以下版本。
            //request.setMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);
            request.setPolicy(POLICY);
            request.setDurationSeconds(durationSeconds);
            final AssumeRoleResponse response = client.getAcsResponse(request);
            System.out.println("Expiration: " + response.getCredentials().getExpiration());
            Date expriation = DateUtil.parseIso8601Date(response.getCredentials().getExpiration());
            System.out.println(expriation);
            Date date = new Date();
            System.out.println((expriation.getTime() - date.getTime())/1000);
            System.out.println("Access Key Id: " + response.getCredentials().getAccessKeyId());
            System.out.println("Access Key Secret: " + response.getCredentials().getAccessKeySecret());
            System.out.println("Security Token: " + response.getCredentials().getSecurityToken());
            System.out.println("RequestId: " + response.getRequestId());
        } catch (ClientException e) {
            System.out.println("Failed：");
            System.out.println("Error code: " + e.getErrCode());
            System.out.println("Error message: " + e.getErrMsg());
            System.out.println("RequestId: " + e.getRequestId());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
