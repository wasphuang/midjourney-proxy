package com.github.novicezk.midjourney.aliyun;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.MD5;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.auth.STSAssumeRoleSessionCredentialsProvider;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
public class OSSFileClient {

    @Autowired
    private StsService stsService;

    // Endpoint以杭州为例，其它Region请按实际情况填写。
    private final String endpoint = "oss-cn-hangzhou.aliyuncs.com";
    private final String bucketName = "prod-midjourney";

    public String uploadImage(String imageURL) {
        HttpURLConnection connection = null;
        OSS ossClient = null;
        try {
            // 填写步骤五获取的临时访问密钥AccessKey ID和AccessKey Secret，非阿里云账号AccessKey ID和AccessKey Secret。
            String accessKeyId = stsService.getOssToken().getAccessKeyId();
            String accessKeySecret = stsService.getOssToken().getAccessKeySecret();
            // 填写步骤五获取的安全令牌SecurityToken。
            String securityToken = stsService.getOssToken().getToken();

            // 创建OSSClient实例。
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);

            URL url = new URL(imageURL);
            connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = outputStream.toByteArray();
                outputStream.close();
                inputStream.close();
                String imageName = DigestUtils.md5DigestAsHex(imageURL.getBytes());
                String fileName = String.format("%s/%s.jpg", DateUtil.format(new Date(),"yyyyMMdd"), imageName);
                // 将本地文件exampletest.txt上传至目标存储空间examplebucket下的src目录。
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, new ByteArrayInputStream(imageBytes));
                // ObjectMetadata metadata = new ObjectMetadata();
                // 上传文件时设置存储类型。
                // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
                // 上传文件时设置读写权限ACL。
                // metadata.setObjectAcl(CannedAccessControlList.Private);
                // putObjectRequest.setMetadata(metadata);
                // 上传文件。
                ossClient.putObject(putObjectRequest);
            } else {
                throw new Exception("图片下载失败，错误码：" + responseCode);
            }
        } catch (Exception e) {
            log.error("OSS转存图片失败：", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (ossClient != null) {
                // 关闭OSSClient。
                ossClient.shutdown();
            }
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        StsService stsService = new StsService();

        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        // 填写步骤五获取的临时访问密钥AccessKey ID和AccessKey Secret，非阿里云账号AccessKey ID和AccessKey Secret。
        String accessKeyId = stsService.getOssToken().getAccessKeyId();
        String accessKeySecret = stsService.getOssToken().getAccessKeySecret();
        // 填写步骤五获取的安全令牌SecurityToken。
        String securityToken = stsService.getOssToken().getToken();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, securityToken);
        String imageURL = "https://resource.threeing.cn/pJz46Tt2aHZqygCKaJw0T/img_5505_9dh0So.webp";

        URL url = new URL(imageURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            outputStream.close();
            inputStream.close();
            String imageName = DigestUtils.md5DigestAsHex(imageURL.getBytes());
            System.out.println(imageName);
            String fileName = String.format("%s/%s.jpg", DateUtil.format(new Date(),"yyyyMMdd"), imageName);
            // 将本地文件exampletest.txt上传至目标存储空间examplebucket下的src目录。
            PutObjectRequest putObjectRequest = new PutObjectRequest("prod-midjourney", fileName, new ByteArrayInputStream(imageBytes));
            // ObjectMetadata metadata = new ObjectMetadata();
            // 上传文件时设置存储类型。
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // 上传文件时设置读写权限ACL。
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);

            // 上传文件。
            ossClient.putObject(putObjectRequest);

            // 关闭OSSClient。
            ossClient.shutdown();

            System.out.println("图片下载完成！");
        } else {
            System.out.println("图片下载失败，错误码：" + responseCode);
        }

        connection.disconnect();

    }
}
