package com.github.novicezk.midjourney.aliyun;

import com.aliyun.oss.common.utils.DateUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.github.novicezk.midjourney.ProxyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class StsService {
    @Autowired
    private  ProxyProperties properties;
    // regionId表示RAM的地域ID。以华东1（杭州）地域为例，regionID填写为cn-hangzhou。也可以保留默认值，默认值为空字符串（""）。
    private final String regionId = "cn-hangzhou";
    private final String product = "Sts";
    // STS接入地址，例如sts.cn-hangzhou.aliyuncs.com。
    private final String endpoint = "sts.cn-hangzhou.aliyuncs.com";
    // 填写步骤1生成的RAM用户访问密钥AccessKey ID和AccessKey Secret。
    private final String accessKeyId = this.properties.getAliyunConfig().getAccessKeyId();
    private final String accessKeySecret = this.properties.getAliyunConfig().getAccessKeySecret();
    // 填写步骤3获取的角色ARN。
    private final String roleArn = "acs:ram::"+this.properties.getAliyunConfig().getUid()+":role/ramosstest";
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
}
