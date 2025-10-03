package io.github.LampEnjoyer.PracticeBot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OpeningBook {
    private Map<String, List<String>> map;

    public OpeningBook () throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.map = mapper.readValue(new File("src/main/java/io/github/LampEnjoyer/PracticeBot/book.json"), new TypeReference<Map<String, List<String>>>(){});
    }

    public boolean isBookPosition(String str){
        return map.containsKey(str);
    }

    public String getMove(String str){
        List<String> list = map.get(str);
        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }

    public Map<String, List<String>> getMap(){
        return map;
    }




}
