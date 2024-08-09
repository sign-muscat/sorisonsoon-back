// package com.sorisonsoon.gameChallenge.service;

// import com.sorisonsoon.common.domain.type.GameDifficulty;
// import com.sorisonsoon.gameChallenge.dto.request.SoundResultRequest;
// import com.sorisonsoon.gameChallenge.dto.response.SoundQuestionResponse;
// import com.sorisonsoon.gameChallenge.domain.repository.GameChallengeRepository;
// import com.sorisonsoon.gameChallenge.dto.response.SoundRecordResponse;
// import com.sorisonsoon.gameChallenge.dto.response.SoundResultResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.List;

// @Service
// @Transactional
// @RequiredArgsConstructor
// public class GameChallengeService {

//     private final GameChallengeRepository gameChallengeRepository;

//     @Transactional(readOnly = true)
//     public SoundQuestionResponse getSoundQuestion(GameDifficulty difficulty) {
//         // TODO: 하루에 한 문제 제한 유무에 따라 로직 변동 가능성 (현재 문제 제한 X)
//         return gameChallengeRepository.getQuestionByDifficulty(difficulty).orElseThrow();
//     }

//     @Transactional(readOnly = true)
//     public List<SoundRecordResponse> getSoundRecords(Long challengeId) {
//         return gameChallengeRepository.getSoundRecords(challengeId);
//     }

//     public SoundResultResponse getResult(SoundResultRequest answerRequest) {

//         // TODO: (1) 전달 받은 문장을 임베딩

//         // TODO: (2) 정답 문장의 임베딩 값과 유사도 판단

//         // TODO: (3) DB record 저장

//         // TODO: (4) SoundResultRequest 에 정답 여부와 유사도 담아서 반환
//         return null;
//     }
// }

package com.sorisonsoon.gameChallenge.service;

import com.sorisonsoon.common.domain.type.GameDifficulty;
import com.sorisonsoon.gameChallenge.dto.request.SoundResultRequest;
import com.sorisonsoon.gameChallenge.dto.response.SoundQuestionResponse;
import com.sorisonsoon.gameChallenge.domain.repository.GameChallengeRepository;
import com.sorisonsoon.gameChallenge.dto.response.SoundRecordResponse;
import com.sorisonsoon.gameChallenge.dto.response.SoundResultResponse;
import lombok.RequiredArgsConstructor;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GameChallengeService {

    private final GameChallengeRepository gameChallengeRepository;
    private Word2Vec word2Vec;

    @PostConstruct
    public void init() throws Exception {
        // Word2Vec 모델 로드 또는 훈련
        File gModel = new File("path/to/GoogleNews-vectors-negative300.bin.gz");
        word2Vec = WordVectorSerializer.readWord2VecModel(gModel);
    }

    @Transactional(readOnly = true)
    public SoundQuestionResponse getSoundQuestion(GameDifficulty difficulty) {
        return gameChallengeRepository.getQuestionByDifficulty(difficulty).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<SoundRecordResponse> getSoundRecords(Long challengeId) {
        return gameChallengeRepository.getSoundRecords(challengeId);
    }

    public SoundResultResponse getResult(SoundResultRequest answerRequest) {
        String userAnswer = answerRequest.getUserAnswer();
        String correctAnswer = answerRequest.getCorrectAnswer();

        // 문장 임베딩
        INDArray userVector = getAverageSentenceVector(userAnswer);
        INDArray correctVector = getAverageSentenceVector(correctAnswer);

        // 코사인 유사도 계산
        double similarity = Transforms.cosineSim(userVector, correctVector);

        // 유사도에 따른 정답 여부 판단 (예: 0.8 이상이면 정답으로 간주)
        boolean isCorrect = similarity >= 0.8;

        // DB record 저장
        gameChallengeRepository.saveRecord(answerRequest, isCorrect, similarity);

        // 결과 반환
        return new SoundResultResponse(isCorrect, similarity);
    }

    private INDArray getAverageSentenceVector(String sentence) {
        String[] words = sentence.toLowerCase().split(" ");
        INDArray sum = null;
        int count = 0;

        for (String word : words) {
            if (word2Vec.hasWord(word)) {
                INDArray wordVector = word2Vec.getWordVectorMatrix(word);
                if (sum == null) {
                    sum = wordVector;
                } else {
                    sum.addi(wordVector);
                }
                count++;
            }
        }

        return (count > 0) ? sum.divi(count) : sum;
    }
}