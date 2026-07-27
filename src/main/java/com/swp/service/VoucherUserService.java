package com.swp.service;

import com.swp.dao.UserDAO;
import com.swp.dao.VoucherDAO;
import com.swp.model.User;
import com.swp.model.Voucher;
import com.swp.model.dto.UserVoucherDTO;
import com.swp.model.dto.VoucherExchangeDTO;
import com.swp.model.dto.VoucherRedeemResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoucherUserService {

    private static final VoucherDAO voucherDao = new VoucherDAO();
    private static final UserDAO userDao = new UserDAO();

    /**
     * Lấy voucher đổi điểm theo đúng quyền xem của Customer.
     * VIP chỉ hợp lệ khi cờ VIP còn hạn, không chỉ dựa vào is_vip trong session.
     */
    public List<VoucherExchangeDTO> getExchangeVouchers(String targetUser,
                                                        User user) {
        boolean activeVip = isVipCurrentlyValid(user);

        List<Voucher> vouchers =
                voucherDao.getAllExchangeVouchers(targetUser, activeVip);

        List<VoucherExchangeDTO> result = new ArrayList<>();

        for (Voucher v : vouchers) {
            VoucherExchangeDTO dto = new VoucherExchangeDTO();
            dto.setId(v.getId());
            dto.setCode(v.getCode());
            dto.setName(v.getName());
            dto.setDiscountType(v.getDiscountType());
            dto.setDiscountValue(v.getDiscountValue());
            dto.setMinOrder(v.getMinOrder());
            dto.setQuantity(v.getQuantity());
            dto.setUsed(v.getUsed());
            dto.setExchangePoints(v.getExchangePoint());
            dto.setEndDate(v.getEndDate());
            dto.setTargetUser(v.getTargetUser());
            dto.setDistributionType(v.getDistributionType());

            result.add(dto);
        }

        return result;
    }

    /**
     * Đổi voucher bằng điểm; DAO tự đọc giá/điểm/quyền từ DB thay vì tin frontend.
     */
    public VoucherRedeemResult redeemVoucher(User user, long voucherId) {
        if (user == null || user.getUserId() == null || !"CUSTOMER".equalsIgnoreCase(user.getRoleName())) {
            return VoucherRedeemResult.failure("Chỉ khách hàng mới được đổi voucher.");
        }
        return voucherDao.redeemVoucher(user.getUserId(), voucherId);
    }

    public List<UserVoucherDTO> getUserVouchers(long userId, String status) {
        return voucherDao.getUserVouchers(userId, status);
    }

    private boolean isVipCurrentlyValid(User user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }

        User latestUser = userDao.getUserById(user.getUserId()).orElse(user);
        return latestUser.isVip()
                && latestUser.getVipValidUntil() != null
                && latestUser.getVipValidUntil().isAfter(LocalDateTime.now());
    }
}
