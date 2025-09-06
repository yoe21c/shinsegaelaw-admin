package com.tbm.admin.exception;

import com.tbm.admin.model.view.rest.RestError;
import com.tbm.admin.service.telegram.TelegramService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private TelegramService telegramService;

    /**
     * 파라미터 검증 실패 예외: 400
     * @param exception
     * @return
     */
    @ExceptionHandler({ TbmAdminRuntimeException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestError tbmRuntimeExceptionHandler(TbmAdminRuntimeException exception) {
        log.error("[! TbmAdminRuntimeException !]: {}", exception.getMessage());
        return new RestError("invalid_request", exception.getMessage());
    }

    /**
     * 최상위 예외: 500
     * @param exception
     * @return
     */
    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestError defaultExceptionHandler(Exception exception, HttpServletRequest request) {
        log.error("[! Exception !]: [{}] -> {}", request.getRequestURI(), exception.getMessage(), exception);

//        telegramService.sendTelegram("500 ERROR ! " + request.getRequestURI() + " " + exception.getMessage());

        return new RestError("server_error", "서버 오류입니다.");
    }

}