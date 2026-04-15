package com.dongjian.erp.manufacturingerpsystem.modules.system.controller;

import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/dicts")
public class DictController {

    @GetMapping("/{dictType}")
    public ApiResponse<List<Map<String, String>>> getDict(@PathVariable String dictType) {
        if ("material_unit".equals(dictType)) {
            return ApiResponse.success(List.of(
                    Map.of("label", "个", "value", "PCS"),
                    Map.of("label", "套", "value", "SET"),
                    Map.of("label", "千克", "value", "KG"),
                    Map.of("label", "米", "value", "M")
            ));
        }
        return ApiResponse.success(List.of());
    }
}
