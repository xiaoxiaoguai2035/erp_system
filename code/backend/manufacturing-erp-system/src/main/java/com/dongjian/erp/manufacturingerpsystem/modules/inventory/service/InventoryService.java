package com.dongjian.erp.manufacturingerpsystem.modules.inventory.service;

import com.dongjian.erp.manufacturingerpsystem.common.page.PageResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryCheckSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.dto.InventoryTransferSaveRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocDetail;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryDocListItem;
import com.dongjian.erp.manufacturingerpsystem.modules.inventory.vo.InventoryStockListItem;

public interface InventoryService {

    PageResponse<InventoryStockListItem> pageStocks(String keyword,
                                                    Long materialId,
                                                    Long warehouseId,
                                                    String lotNo,
                                                    long pageNo,
                                                    long pageSize);

    PageResponse<InventoryDocListItem> pageDocs(String keyword,
                                                String docType,
                                                String status,
                                                Long warehouseId,
                                                long pageNo,
                                                long pageSize);

    InventoryDocDetail getDoc(Long id);

    PageResponse<InventoryDocListItem> pageTransfers(String keyword, String status, Long warehouseId, long pageNo, long pageSize);

    InventoryDocDetail getTransfer(Long id);

    Long createTransfer(InventoryTransferSaveRequest request);

    void approveTransfer(Long id);

    PageResponse<InventoryDocListItem> pageChecks(String keyword, String status, Long warehouseId, long pageNo, long pageSize);

    InventoryDocDetail getCheck(Long id);

    Long createCheck(InventoryCheckSaveRequest request);

    void approveCheck(Long id);
}
