package com.swp.service;

import com.swp.dao.UserDAO;
import com.swp.dao.VoucherDAO;
import com.swp.model.User;
import com.swp.model.Voucher;
import com.swp.model.dto.UserVoucherDTO;
import com.swp.model.dto.VoucherExchangeDTO;

import java.util.ArrayList;
import java.util.List;

public class VoucherUserService {

    private static final VoucherDAO voucherDao = new VoucherDAO();
    private static final UserDAO userDao = new UserDAO();

    public List<VoucherExchangeDTO> getExchangeVouchers(String targetUser,
                                                        boolean isVip) {

        List<Voucher> vouchers =
                voucherDao.getAllExchangeVouchers(targetUser, isVip);

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

            result.add(dto);
        }

        return result;
    }

    public boolean redeemVoucher(User user, long voucherId) {
        return voucherDao.redeemVoucher(user.getUserId(), voucherId);
    }

    public List<UserVoucherDTO> getUserVouchers(long userId, String status) {
        return voucherDao.getUserVouchers(userId, status);
    }
}
