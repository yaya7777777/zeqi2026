package com.zeqi.usermanagement.service;

import com.zeqi.usermanagement.dto.AccountDTOs;
import com.zeqi.usermanagement.dto.LoginDTO;
import com.zeqi.usermanagement.dto.LoginVO;
import com.zeqi.usermanagement.dto.RegisterDTO;
import com.zeqi.usermanagement.dto.UserVO;
import com.zeqi.usermanagement.entity.User;
import com.zeqi.usermanagement.exception.BusinessException;
import com.zeqi.usermanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * =====================================================================================================================
 * 🧠 UserService = 业务逻辑层（所有"业务规则/if判断/数据组合"全部写这一层）
 * =====================================================================================================================
 * 🏷️ @Service = Spring IOC 注册 Service Bean
 * 🏷️ @Transactional = 声明式事务（方法执行前 BEGIN，方法正常结束 COMMIT，抛异常自动 ROLLBACK）
 *     注册 / 改密码 / 注销这种写操作都应该加事务，保证"要么全成功要么全失败"ACID。
 *
 * 🎯 分层思想（面试必背！）：
 *   Controller 接收 HTTP 请求 → 交给 Service 处理业务 → Service 调 Repository 写 SQL → 返回 Entity/DTO
 *   严格禁止：Controller 里写 SQL / Service 里拿 request 直接读（职责单一原则 = 每一层只管自己的事）
 */
@Service
public class UserService {

    /** 依赖 Repository（构造器注入） + SessionManager（会话存储） */
    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public UserService(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    /* ===============================================================================================================
     * 1️⃣ 用户注册 → 返回新建的 UserVO（安全过滤掉 password）
     * =============================================================================================================== */
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // ① 业务校验：UNIQUE 冲突 → 409 Conflict（先查再插；也可以直接插捕获 DuplicateKeyException，可读性优先选先查）
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw BusinessException.conflict("用户名 '" + dto.getUsername() + "' 已存在，请换一个");
        }

        // ② 组装 User Entity（注意：题目允许明文存密码！生产必须 BCryptPasswordEncoder.encode(rawPwd)）
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(dto.getPassword());

        // ③ 插入 DB → 拿到自增主键
        Long newId = userRepository.insert(user);

        // ④ 查回完整用户 → 转 UserVO 脱敏 → 返回给 Controller
        User inserted = userRepository.findById(newId)
                .orElseThrow(() -> BusinessException.notFound("注册后查询用户失败"));
        return new UserVO(inserted);
    }

    /* ===============================================================================================================
     * 2️⃣ 用户登录 → 返回 LoginVO（UserVO 的基础上 + sessionId）
     * =============================================================================================================== */
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername().trim();

        // ① 按用户名查用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));

        // ② 密码比对（不区分不存在和密码错 → 统一 401，防爆破枚举用户名）
        if (!dto.getPassword().equals(user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // ③ 生成 UUID 随机 sessionId（= Python 版 uuid.uuid4()）
        String sessionId = UUID.randomUUID().toString();

        // ④ 存进 SessionManager（全局 Map<sessionId, userId>，等价 Python Sessions dict）
        sessionManager.save(sessionId, user.getId());

        // ⑤ 组装 LoginVO 返回
        LoginVO vo = new LoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        vo.setSessionId(sessionId);
        return vo;
    }

    /* ===============================================================================================================
     * 3️⃣ 查看当前登录用户的信息 → UserVO
     * =============================================================================================================== */
    public UserVO getCurrentUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在（可能已注销）"));
        return new UserVO(user);
    }

    /* ===============================================================================================================
     * 4️⃣ （加分）查看用户列表 → List<UserVO> （加 total 交给 Controller 拼 Result）
     * =============================================================================================================== */
    public List<UserVO> listAllUsers() {
        List<User> users = userRepository.findAllOrderById();
        // Stream API：集合流管道式处理，map(UserVO::new) = 每个 User 转 UserVO，等价 Python [UserVO(u) for u in users]
        return users.stream().map(UserVO::new).collect(Collectors.toList());
    }

    /* ===============================================================================================================
     * 5️⃣ （加分）修改用户名 → 返回更新后 UserVO
     * =============================================================================================================== */
    @Transactional
    public UserVO updateUsername(Long currentUserId, AccountDTOs.UpdateUsernameDTO dto) {
        String newName = dto.getUsername().trim();

        // ① 先查重：有没有"其他用户"已经叫这个名（排除自己，不然自己的用户名和自己重名永远 409）
        Integer count = userRepository.countByUsernameAndIdNot(newName, currentUserId);
        if (count > 0) {
            throw BusinessException.conflict("用户名 '" + newName + "' 已被占用");
        }

        // ② 执行 UPDATE
        int rows = userRepository.updateUsername(currentUserId, newName);
        if (rows != 1) {
            throw BusinessException.notFound("用户不存在，修改用户名失败");
        }

        // ③ 查回最新 → 返回
        return new UserVO(userRepository.findById(currentUserId).orElseThrow());
    }

    /* ===============================================================================================================
     * 6️⃣ （加分）修改密码 → 成功后【踢掉该用户所有登录设备】（清空所有他的 sessionId）
     * =============================================================================================================== */
    @Transactional
    public void updatePassword(Long currentUserId, AccountDTOs.UpdatePasswordDTO dto) {
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw BusinessException.badRequest("新密码不能和原密码相同");
        }

        // ① 确认原密码正确
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (!dto.getOldPassword().equals(user.getPassword())) {
            throw BusinessException.unauthorized("原密码错误");
        }

        // ② 更新密码
        int rows = userRepository.updatePassword(currentUserId, dto.getNewPassword());
        if (rows != 1) {
            throw BusinessException.notFound("密码更新失败");
        }

        // ③ 【安全加固】改密码后强制下线：删除这个用户所有 session（和 Python 版"清 Sessions 同 userId 的项"一致）
        sessionManager.invalidateAllByUserId(currentUserId);
    }

    /* ===============================================================================================================
     * 7️⃣ （加分）注销账号 → 删 DB 行 + 清 session（二次确认密码）
     * =============================================================================================================== */
    @Transactional
    public void deleteAccount(Long currentUserId, AccountDTOs.DeleteAccountDTO dto) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        // 二次确认密码
        if (!dto.getPassword().equals(user.getPassword())) {
            throw BusinessException.unauthorized("密码确认失败，账号已保留");
        }

        // 删 DB
        int rows = userRepository.deleteById(currentUserId);
        if (rows != 1) {
            throw BusinessException.notFound("注销失败，用户不存在");
        }

        // 踢下线
        sessionManager.invalidateAllByUserId(currentUserId);
    }
}
