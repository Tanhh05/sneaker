package com.sneakerstore.service;

import com.sneakerstore.common.exception.BusinessException;
import com.sneakerstore.common.exception.ResourceNotFoundException;
import com.sneakerstore.common.util.SecurityUtils;
import com.sneakerstore.entity.*;
import com.sneakerstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SoftDeleteService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void softDeletePurchase(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập ID: " + id));
        po.setDeleted(true);
        po.setUpdatedBy(SecurityUtils.getCurrentUsername());
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public void softDeleteSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp ID: " + id));
        s.setDeleted(true);
        s.setStatus("INACTIVE");
        s.setUpdatedBy(SecurityUtils.getCurrentUsername());
        supplierRepository.save(s);
    }

    @Transactional
    public void softDeleteCustomer(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + id));
        c.setDeleted(true);
        c.setStatus("INACTIVE");
        c.setUpdatedBy(SecurityUtils.getCurrentUsername());
        customerRepository.save(c);
    }

    @Transactional
    public void softDeleteBrand(Long id) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + id));
        b.setDeleted(true);
        b.setStatus("INACTIVE");
        b.setUpdatedBy(SecurityUtils.getCurrentUsername());
        brandRepository.save(b);
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + id));
        String actor = SecurityUtils.getCurrentUsername();
        p.setDeleted(true);
        p.setStatus("INACTIVE");
        p.setUpdatedBy(actor);
        productRepository.save(p);
    }

    @Transactional
    public void softDeleteVariant(Long id) {
        ProductVariant pv = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể ID: " + id));
        pv.setDeleted(true);
        pv.setStatus("INACTIVE");
        pv.setUpdatedBy(SecurityUtils.getCurrentUsername());
        productVariantRepository.save(pv);
    }

    @Transactional
    public void softDeleteEmployee(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên ID: " + id));
        e.setDeleted(true);
        e.setStatus("INACTIVE");
        e.setUpdatedBy(SecurityUtils.getCurrentUsername());
        employeeRepository.save(e);
    }

    @Transactional
    public void softDeleteUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ID: " + id));
        String current = SecurityUtils.getCurrentUsername();
        if ("admin".equalsIgnoreCase(u.getUsername()) || current.equalsIgnoreCase(u.getUsername())) {
            throw new BusinessException("Không thể xóa tài khoản quản trị mặc định hoặc tài khoản đang đăng nhập!");
        }
        u.setDeleted(true);
        u.setStatus("LOCKED");
        u.setUpdatedBy(current);
        userRepository.save(u);
    }
}
