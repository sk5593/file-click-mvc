package com.file.click.mvc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
@RequestMapping( "/api" )
public class FileController {
    OkHttpClient client = new OkHttpClient();

    /**
     * 文件上传接口
     * @param multipartFile
     * @return
     * @throws Exception
     */
    @RequestMapping( "/upload" )
    public R fileUpload(@RequestParam( "file" ) MultipartFile multipartFile) throws Exception {
        try {
            File toFile = null;
            if (multipartFile.equals("") || multipartFile.getSize() <= 0) {
                multipartFile = null;
            } else {
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
                    .header("X-Signature", RsaUtils.encrypt(uuid, MainConfig.PRIVATE_KEY))
                    .url(MainConfig.URL + "/api/upload")
                    .post(requestBody)
                    .build();
            Response response = client.newCall(requestOk).execute();
            String responseBody = response.body().string();
            if (responseBody.equals("403") || responseBody.equals("500")) {
                log.error("调用接口失败");
                return R.fail();
            }
            return new R(200, "success", responseBody);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail();
        }
    }

    /**
     * MultipartFile转为file使用
     * @param ins
     * @param file
     */
    public static void inputStreamToFile(InputStream ins, File file) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

        } catch (Exception e) {
          log.error("inputStreamToFile调用失败" , e);
        } finally {
            if (os != null)
            os.close();
            if (ins != null)
            ins.close();
        }
    }

    /**
     * 获取文件详情接口
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping( "/getDetails" )
    public R getDetails(String id) throws Exception {
        JSONObject jsonObject = getById(id);
        if (Objects.nonNull(jsonObject)) {
            return new R(200, "success", jsonObject);
        }
        return R.fail();
    }

    /**
     * 文件下载
     * @param id：uuid
     * @param response
     * @return
     * @throws Exception
     */
    @RequestMapping( "/download" )
    public R downLoad(String id, HttpServletResponse response) throws Exception {
        JSONObject jsonObject = getById(id);
        ServletOutputStream out = null;
        CipherOutputStream cout = null;
        FileInputStream in = null;
        try {
            if (Objects.nonNull(jsonObject)) {
                String envelope = jsonObject.getString("envelope");
                String dir = jsonObject.getString("dir");
                String fileName = jsonObject.getString("name");
                //私钥解密对称密钥
                String key = RsaUtils.decrypt(envelope, MainConfig.PRIVATE_KEY);
                response.setContentType("multipart/form-data");
                //response.setContentType("multipart/form-data;charset=UTF-8");也可以明确的设置一下UTF-8，测试中不设置也可以。
                response.setHeader("Content-Disposition", "attachment; fileName=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
                //用对称密钥key解密文件
                in = new FileInputStream(new File(dir));
                out = response.getOutputStream();
                Key k = AesUtils.toKey(key.getBytes());
                byte[] raw = k.getEncoded();
                SecretKeySpec secretKeySpec = new SecretKeySpec(raw, MainConfig.ALGORITHM);
                Cipher cipher = Cipher.getInstance(MainConfig.ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                cout = new CipherOutputStream(out, cipher);
                byte[] cache = new byte[MainConfig.CACHE_SIZE];
                int nRead = 0;
                while ((nRead = in.read(cache)) != -1) {
                    cout.write(cache, 0, nRead);
                    cout.flush();
                }
            }
        }catch (Exception e){
            log.error("文件下载异常" , e);
            return new R(410,"fail");
        }finally {
            if (cout != null)
            cout.close();
            if (out != null)
            out.close();
            if (in != null)
            in.close();
        }
        return null;
    }

    /**
     * 根据uuid获取详细信息
     * @param id
     * @return
     * @throws Exception
     */
    public JSONObject getById(String id) throws Exception {
        try {
            String uuid = UUID.randomUUID().toString();
            Request request = new Request.Builder()
                    .header("X-SID", uuid)
                    .header("X-Signature", RsaUtils.encrypt(uuid, MainConfig.PRIVATE_KEY))
                    .url(MainConfig.URL + "/api/getDetails?id=" + id)
                    .build();
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            if (responseBody.equals("403") || responseBody.equals("500")) {
                log.error("调用接口失败");
                return null;
            }
            JSONObject jsonObject = JSON.parseObject(responseBody.toString());
            return jsonObject;
        } catch (Exception e) {
            log.error("getById方法异常", e);
            return null;
        }
    }

    /**
     * 获取最近上传列表接口
     * @return
     * @throws Exception
     */
    @RequestMapping( "/getList" )
    public R getList() throws Exception {
        try {
            String uuid = UUID.randomUUID().toString();
            Request request = new Request.Builder()
                    .header("X-SID", uuid)
                    .header("X-Signature", RsaUtils.encrypt(uuid, MainConfig.PRIVATE_KEY))
                    .url(MainConfig.URL + "/api/getList")
                    .build();
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            if (Objects.nonNull(responseBody)) {
                //json字符串转json数组
                JSONArray jsonArray = JSON.parseArray(responseBody.toString());
                return new R(200, "success", jsonArray);
            }
        } catch (Exception e) {
            log.error("getList方法异常", e);
            return R.fail();
        }
        return R.fail();
    }
}
