package com.soodfolio.api.service;

import com.soodfolio.api.dto.AiDtos.AiPolishRequestDto;
import com.soodfolio.api.dto.AiDtos.AiPolishResponseDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestRequestDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestResponseDto;

public interface AiService {

    AiPolishResponseDto polishText(AiPolishRequestDto request);

    AiThemeSuggestResponseDto suggestTheme(AiThemeSuggestRequestDto request);
}
