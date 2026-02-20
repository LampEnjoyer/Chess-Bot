package io.github.LampEnjoyer.PracticeBot.engine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Opening {
    private Map<String, List<String>> map;
    private List<String> games;
    private final String fileName = "Games.txt";

    public Opening () throws IOException {
        games = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        while (true) {
            String curr = br.readLine();
            if (curr != null) {
                games.add(curr);
            } else {
                br.close();
                br = new BufferedReader(new FileReader(fileName));
                break;
            }
        }
    }

    public List<String> getGames() {
        return games;
    }
}
