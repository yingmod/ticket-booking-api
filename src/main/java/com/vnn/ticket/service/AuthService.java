package com.vnn.ticket.service;

import com.vnn.ticket.dto.request.LoginRequestDto;
import com.vnn.ticket.dto.request.RegisterRequestDto;
import com.vnn.ticket.dto.response.AuthResponseDto;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
}
