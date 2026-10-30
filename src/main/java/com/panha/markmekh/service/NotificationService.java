package com.panha.markmekh.service;

import com.panha.markmekh.dto.NotificationDto;
import com.panha.markmekh.entity.AttendanceRecord;
import com.panha.markmekh.entity.Employee;
import com.panha.markmekh.entity.LeaveRequest;
import com.panha.markmekh.entity.Notification;
import com.panha.markmekh.entity.OvertimeRequest;
import com.panha.markmekh.entity.Role;
import com.panha.markmekh.entity.User;
import com.panha.markmekh.repository.AttendanceRecordRepository;
import com.panha.markmekh.repository.EmployeeRepository;
import com.panha.markmekh.repository.LeaveRequestRepository;
import com.panha.markmekh.repository.NotificationRepository;
import com.panha.markmekh.repository.OvertimeRequestRepository;
import com.panha.markmekh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;
    private final EmployeeRepository employeeRepo;
    // 👇 Added repositories to look up the specific requests
    private final AttendanceRecordRepository attendanceRepo;
    private final LeaveRequestRepository leaveRepo;
    private final OvertimeRequestRepository overtimeRepo;

    public NotificationService(NotificationRepository notifRepo,
                               UserRepository userRepo,
                               EmployeeRepository employeeRepo,
                               AttendanceRecordRepository attendanceRepo,
                               LeaveRequestRepository leaveRepo,
                               OvertimeRequestRepository overtimeRepo) {
        this.notifRepo = notifRepo;
        this.userRepo = userRepo;
        this.employeeRepo = employeeRepo;
        this.attendanceRepo = attendanceRepo;
        this.leaveRepo = leaveRepo;
        this.overtimeRepo = overtimeRepo;
    }

    @Transactional
    public void notifyUser(User user, String type, String title,
                           String message, Long referenceId) {
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .read(false)
                .build();
        notifRepo.save(n);
    }

    @Transactional
    public void notifyAdmins(String type, String title,
                             String message, Long referenceId) {
        List<User> admins = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN
                        || u.getRole() == Role.HR_MANAGER
                        || u.getRole() == Role.MANAGER)
                .collect(Collectors.toList());
        for (User u : admins) {
            notifyUser(u, type, title, message, referenceId);
        }
    }

    @Transactional
    public void notifyManagers(String type, String title,
                               String message, Long referenceId) {
        List<User> managers = userRepo.findAll().stream()
                .filter(u -> u.getRole() == Role.MANAGER
                        || u.getRole() == Role.HR_MANAGER
                        || u.getRole() == Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        for (User u : managers) {
            notifyUser(u, type, title, message, referenceId);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listForUser(Long userId) {
        return notifRepo.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifRepo.countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notifRepo.findById(notificationId).orElse(null);
        if (n == null) return;
        if (!n.getUser().getId().equals(userId)) return;
        n.setRead(true);
        notifRepo.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> list =
                notifRepo.findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : list) n.setRead(true);
        notifRepo.saveAll(list);
    }

    private NotificationDto toDto(Notification n) {
        String profilePhotoUrl = null;

        try {
            // 1. Check Attendance Alerts
            if (n.getType().contains("ATTENDANCE") && n.getReferenceId() != null) {
                AttendanceRecord record = attendanceRepo.findById(n.getReferenceId()).orElse(null);
                if (record != null && record.getEmployee() != null) {
                    profilePhotoUrl = record.getEmployee().getProfilePhotoUrl();
                }
            }
            // 2. Check Leave Requests
            else if (n.getType().startsWith("LEAVE") && n.getReferenceId() != null) {
                LeaveRequest request = leaveRepo.findById(n.getReferenceId()).orElse(null);
                if (request != null && request.getEmployee() != null) {
                    profilePhotoUrl = request.getEmployee().getProfilePhotoUrl();
                }
            }
            // 3. Check Overtime Requests
            else if (n.getType().startsWith("OVERTIME") && n.getReferenceId() != null) {
                OvertimeRequest request = overtimeRepo.findById(n.getReferenceId()).orElse(null);
                if (request != null && request.getEmployee() != null) {
                    profilePhotoUrl = request.getEmployee().getProfilePhotoUrl();
                }
            }
            // 4. Fallback: Just get the notification recipient's photo (for other alerts)
            else if (n.getUser() != null) {
                Employee emp = employeeRepo.findByUser_Id(n.getUser().getId()).orElse(null);
                if (emp != null) {
                    profilePhotoUrl = emp.getProfilePhotoUrl();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch profile photo for notification: " + e.getMessage());
        }

        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .profilePhotoUrl(profilePhotoUrl)
                .build();
    }
}