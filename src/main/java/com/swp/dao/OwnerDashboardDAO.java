package com.swp.dao;

import com.swp.model.dto.RevenueDTO;
import com.swp.util.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OwnerDashboardDAO {

    public int getTodayBooking() {
        String sql = """
                    SELECT COUNT(*)
                    FROM bookings
                    WHERE CAST(created_at AS DATE) = CAST(GETDATE() AS DATE)
                    AND status = 'COMPLETED'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getYesterdayBooking() {
        String sql = """
                    SELECT COUNT(*)
                    FROM bookings
                    WHERE CAST(created_at AS DATE)
                        = CAST(DATEADD(DAY,-1,GETDATE()) AS DATE)
                    AND status = 'COMPLETED'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getCurrentMonthRevenue() {
        String sql = """
                    SELECT ISNULL(SUM(total_amount),0)
                    FROM bookings
                    WHERE YEAR(created_at)=YEAR(GETDATE())
                    AND MONTH(created_at)=MONTH(GETDATE())
                    AND status='COMPLETED'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public long getPreviousMonthRevenue() {
        String sql = """
                SELECT ISNULL(SUM(total_amount),0)
                FROM bookings
                WHERE YEAR(created_at)=YEAR(DATEADD(MONTH,-1,GETDATE()))
                AND MONTH(created_at)=MONTH(DATEADD(MONTH,-1,GETDATE()))
                AND status='COMPLETED'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getActiveFields() {
        String sql = """
                    SELECT COUNT(*)
                    FROM fields
                    WHERE status NOT IN ('INACTIVE', 'MAINTENANCE')
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTotalFields() {
        String sql = """
                    SELECT COUNT(*)
                    FROM fields
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<RevenueDTO> getRevenueLast7Days() {
        List<RevenueDTO> list = new ArrayList<>();
        String sql = """
                    SELECT
                        CAST(created_at AS DATE) AS booking_day,
                        SUM(total_amount) AS revenue
                    FROM bookings
                    WHERE created_at >= DATEADD(DAY,-6,GETDATE())
                    AND status='COMPLETED'
                    GROUP BY CAST(created_at AS DATE)
                    ORDER BY booking_day
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(
                        new RevenueDTO(
                                rs.getDate("booking_day")
                                        .toLocalDate(),
                                rs.getLong("revenue")
                        )
                );

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

}
