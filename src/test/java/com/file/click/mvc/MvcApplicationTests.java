package com.file.click.mvc;

import com.file.click.mvc.config.MainConfig;
import com.file.click.mvc.config.R;
import com.file.click.mvc.controller.FileController;
import com.file.click.mvc.util.RsaUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MvcApplicationTests {
	@Autowired
	FileController fileController;
	@Test
	void contextLoads() {
	}
	@Test
	void getDetails() throws Exception {
        R list = fileController.getList();
        System.out.println(list.object.toString());
    }

}
