package com.file.click.mvc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.file.click.mvc.config.MainConfig;
import com.file.click.mvc.config.R;
import com.file.click.mvc.util.AesUtils;
import com.file.click.mvc.util.RsaUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.security.Key;
import java.util.Objects;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/api")
public class FileController {
    OkHttpClient client = new OkHttpClient();
    @RequestMapping("/upload")
    public R fileUpload(@RequestParam( "file" ) MultipartFile multipartFile) throws Exception {
        try {
            log.info("调用文件上传接口");
            File toFile = null;
            if(multipartFile.equals("")||multipartFile.getSize()<=0){
                multipartFile = null;
            }else {
                InputStream ins = null;
                ins = multipartFile.getInputStream();
                toFile = new File(multipartFile.getOriginalFilename());
                inputStreamToFile(ins, toFile);
                ins.close();
            }
            String uuid = UUID.randomUUID().toString();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MediaType.parse("multipart/form-data"))
                    .addFormDataPart("file", toFile.getName(), RequestBody.create(MediaType.parse("text/x-markdown; charset=utf-8"), toFile)) //文件一
                    .build();
            Request requestOk = new Request.Builder()
                    .header("X-SID", uuid)
                    .header("X-Signature", RsaUtils.encrypt(uuid,MainConfig.PRIVATE_KEY))
                    .url(MainConfig.URL + "/api/upload")
                    .post(requestBody)
                    .build();
            Response response = client.newCall(requestOk).execute();
            String responseBody = response.body().string();
            if (responseBody.equals("403") || responseBody.equals("500")){
                log.error("调用接口失败");
                return R.fail();
            }
            return new R(200,"success",responseBody);
        } catch (Exception e){
            log.error("接口调用失败" , e);
            return R.fail();
        }
    }
    public static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequestMapping("/getDetails")
    public R getDetails(String id) throws Exception {
        JSONObject jsonObject = getById(id);
        if (Objects.nonNull(jsonObject)){
            return new R(200,"success",jsonObject);
        }
        return R.fail();
    }
    @RequestMapping("/download")
    public R downLoad(String id, HttpServletResponse response) throws Exception {
        JSONObject jsonObject = getById(id);


        if (Objects.nonNull(jsonObject)){
            String envelope = jsonObject.getString("envelope");
            String dir = jsonObject.getString("dir");
            String fileName = jsonObject.getString("name");
            String key = RsaUtils.decrypt(envelope, MainConfig.PRIVATE_KEY);
            response.setContentType("multipart/form-data");
            //response.setContentType("multipart/form-data;charset=UTF-8");也可以明确的设置一下UTF-8，测试中不设置也可以。
            response.setHeader("Content-Disposition", "attachment; fileName="+  fileName +";filename*=utf-8''"+URLEncoder.encode(fileName,"UTF-8"));

//            byte[] buffer = new byte[1024];
//            FileInputStream fis = null;
//            BufferedInputStream bis = null;
//            try {
//                fis = new FileInputStream(new File(dir));
//                bis = new BufferedInputStream(fis);
//                OutputStream os = response.getOutputStream();
//                int i = bis.read(buffer);
//                while (i != -1) {
//                    os.write(buffer, 0, i);
//                    i = bis.read(buffer);
//                }
//
//            } catch (Exception e){
//
//            }
//            用对称加密的密钥解密文件
            FileInputStream in = new FileInputStream(new File(dir));
            ServletOutputStream out = response.getOutputStream();
            Key k = AesUtils.toKey(key.getBytes());
            byte[] raw = k.getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, MainConfig.ALGORITHM);
            Cipher cipher = Cipher.getInstance(MainConfig.ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            CipherOutputStream cout = new CipherOutputStream(out, cipher);
            byte[] cache = new byte[MainConfig.CACHE_SIZE];
            int nRead = 0;
            while ((nRead = in.read(cache)) != -1) {
                cout.write(cache, 0, nRead);
                cout.flush();
            }
            cout.close();
            out.close();
            in.close();
        }
        return null;

    }
    public JSONObject getById(String id) throws Exception {
        String uuid = UUID.randomUUID().toString();
        Request request = new Request.Builder()
                .header("X-SID", uuid)
                .header("X-Signature", RsaUtils.encrypt(uuid,MainConfig.PRIVATE_KEY))
                .url(MainConfig.URL + "/api/getDetails?id="+id)
                .build();
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        if (responseBody.equals("403") || responseBody.equals("500")){
            log.error("调用接口失败");
            return null;
        }
        JSONObject jsonObject = JSON.parseObject(responseBody.toString());
        return jsonObject;

    }
}
