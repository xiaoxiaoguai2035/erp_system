package com.dongjian.erp.manufacturingerpsystem;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(
        basePackages = "com.dongjian.erp.manufacturingerpsystem.modules",
        markerInterface = BaseMapper.class
)
public class ManufacturingErpSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManufacturingErpSystemApplication.class, args);
    }

}
