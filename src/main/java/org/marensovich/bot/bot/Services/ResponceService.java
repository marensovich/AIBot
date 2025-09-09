package org.marensovich.bot.bot.Services;

import org.marensovich.bot.bot.Database.Models.Responce;
import org.marensovich.bot.bot.Database.Repositories.ResponceRepository;
import org.marensovich.bot.bot.Services.AI.GPT.Data.AIModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ResponceService {

    @Autowired private ResponceRepository responceRepository;

    public void saveResponce(
            Long userId,
            String requestMessage,
            String responceMessage,
            int requestTokens,
            int responceTokens,
            int totalTokens,
            AIModels modelType,
            AIModels.DeepSeekModels deepSeekModel,
            AIModels.YandexModels yandexModel
    ){
        Responce responce = new Responce();
        responce.setUserId(userId);
        responce.setRequestMessage(requestMessage);
        responce.setResponceMessage(responceMessage);
        responce.setRequestTokens(BigDecimal.valueOf(requestTokens));
        responce.setResponseTokens(BigDecimal.valueOf(responceTokens));
        responce.setTotalTokens(BigDecimal.valueOf(totalTokens));
        responce.setModelType(modelType);
        responce.setDeepSeekModels(deepSeekModel);
        responce.setYandexModels(yandexModel);

        responceRepository.save(responce);
    }

}
