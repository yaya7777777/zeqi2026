package com.zeqi.usermanagement.config;

import com.zeqi.usermanagement.dto.Result;
import com.zeqi.usermanagement.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * =====================================================================================================================
 * 🚨 GlobalExceptionHandler = 全局异常处理器（= Python 里 @app.errorhandler(500) / Flask Blueprint 的 errorhandler）
 * =====================================================================================================================
 * 🏷️ @RestControllerAdvice = @ControllerAdvice + @ResponseBody 合体，所有 @ExceptionHandler 方法都直接返回 JSON
 *   作用：Controller/Service 里抛的任何异常，只要在这个类里写了对应的 @ExceptionHandler 就能捕获
 *   统一把错误包装成 Result.fail(code, message)，保证前端拿到的 JSON 结构永远一致
 *
 * 💡 好处（为什么用全局异常而不是每个 Controller 写 try/catch？）
 *   ① 0 重复代码：不用每个接口 try/catch BusinessException 然后 return Result.fail
 *   ② 防止漏网之鱼：RuntimeException/NullPointerException 这种你忘了 catch 的也会兜住，返回 500 统一格式，
 *      不会给前端一堆 JVM 堆栈字符串（堆栈只打在服务端 log 里）
 */
@Slf4j   // Lombok 注解：自动生成 private static final Logger log = LoggerFactory.getLogger(...)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** ① 业务自定义异常（最常见：400/401/404/409） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** ② @Valid / @Validated 注解在 @RequestBody DTO 校验失败（最常见：@NotBlank 没传） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(Exception e) {
        String msg;
        if (e instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors()
                    .stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("；"));
        } else {
            msg = ((BindException) e).getBindingResult().getFieldErrors()
                    .stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("；"));
        }
        log.warn("[参数校验失败] {}", msg);
        return Result.fail(400, msg);
    }

    /** ③ @Validated 路径变量/单参数校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage()).collect(Collectors.joining("；"));
        return Result.fail(400, msg);
    }

    /** ④ 请求体 JSON 解析失败：Body 是空 / JSON 语法错误 / Content-Type 不是 application/json */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[请求体不可读] {}", e.getMessage());
        return Result.fail(400, "请求体格式错误（请检查 JSON 语法 / Content-Type: application/json）");
    }

    /** ⑤ 兜底：所有其他 RuntimeException / Exception（空指针/数组越界/DB 异常等都兜住，返回 500 不暴露堆栈给前端） */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAll(Exception e) {
        log.error("[未预期异常]", e);
        return Result.fail(500, "服务器内部错误，请联系管理员（详情请查看服务端日志）");
    }
}
