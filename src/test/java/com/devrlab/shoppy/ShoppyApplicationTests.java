package com.devrlab.shoppy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ShoppyApplicationTests {

	@Test
	void contextLoads() {
	}

}
