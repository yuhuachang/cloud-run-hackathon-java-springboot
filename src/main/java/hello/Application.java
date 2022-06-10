package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class Application {

//    public static void main(String[] args) {
//        SpringApplication.run(Application.class, args);
//    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/")
    public String index() {
        return "Let the battle begin!";
    }

    @PostMapping("/**")
    public String index(@RequestBody ArenaUpdate arenaUpdate) {

        int width = arenaUpdate.arena.dims.get(0);
        int height = arenaUpdate.arena.dims.get(1);

        int[][] gameBoard = calculateGameBoard(arenaUpdate);
        printGameBoard(arenaUpdate, gameBoard);

        // myself
        String myUrl = arenaUpdate._links.self.href;
        PlayerState myself = arenaUpdate.arena.state.get(myUrl);
        int currentValue = gameBoard[myself.x][myself.y];
        System.out.println(currentValue + " " + myself.direction + "(" + myself.x + ", " + myself.y + ") <--- current");

        // avoid risk
        List<PlayerStateWrapper> list = new LinkedList<>();
        List<PlayerState> possibleStates = possiblePlayerState(myself);
        for (PlayerState nextState : possibleStates) {
            if (nextState.x < 0 || nextState.x >= width) continue;
            if (nextState.y < 0 || nextState.y >= height) continue;
            int nextValue = gameBoard[nextState.x][nextState.y];
            String nextAction = getAction(myself, nextState);
            if (nextValue <= 0) continue;
            list.add(new PlayerStateWrapper(nextState, nextValue, nextAction));
        }
        list.sort((a, b) -> b.value - a.value);
        System.out.println(list);

        PlayerStateWrapper bestAction = list.get(0);
        if (bestAction.value > 2 && hasTarget(gameBoard, myself)) {
            System.out.println("Attack!!!");
            return "T";
        }
        System.out.println("Run!!! Go " + bestAction.action);
        return bestAction.action;

    }

    public static class PlayerStateWrapper {
        public PlayerState state;
        public int value;
        public String action;
        public PlayerStateWrapper(PlayerState state, int value, String action) {
            this.state = state;
            this.value = value;
            this.action = action;
        }
        @Override
        public String toString() {
            return value + " " + state.direction + "(" + state.x + ", " + state.y + ") --> " + action;
        }
    }

    public void printPossibleDecision(int[][] gameBoard, PlayerState myself, PlayerState nextState) {
        try {
            String action = getAction(myself, nextState);
            int nextValue = gameBoard[nextState.x][nextState.y];
            System.out.println(nextValue + " " + nextState.direction + "(" + nextState.x + ", " + nextState.y + ") ===> " + action);
        } catch (ArrayIndexOutOfBoundsException e) {
            // ignore
        }
    }

    public String getDefaultAction(ArenaUpdate arenaUpdate) {
        String myUrl = arenaUpdate._links.self.href;
        PlayerState myself = arenaUpdate.arena.state.get(myUrl);
        int width = arenaUpdate.arena.dims.get(0);
        int height = arenaUpdate.arena.dims.get(1);

        if ("N".equals(myself.direction)) {
            if (myself.y - ATTACH_RANGE <= 0) {
                if (myself.x > width / 2) {
                    return "L";
                } else {
                    return "R";
                }
            }
        } else if ("E".equals(myself.direction)) {
            if (myself.x + ATTACH_RANGE > width) {
                if (myself.y > height / 2) {
                    return "L";
                } else {
                    return "R";
                }
            }
        } else if ("S".equals(myself.direction)) {
            if (myself.y + ATTACH_RANGE > height) {
                if (myself.x > width / 2) {
                    return "R";
                } else {
                    return "L";
                }
            }
        } else if ("W".equals(myself.direction)) {
            if (myself.x - ATTACH_RANGE <= 0) {
                if (myself.y > height / 2) {
                    return "R";
                } else {
                    return "L";
                }
            }
        }
        return "F";
    }

    public boolean hasTarget(int[][] gameBoard, PlayerState myself) {
        if ("N".equals(myself.direction)) {
            for (int i = 0; i <= ATTACH_RANGE; i++) {
                int y = myself.y - i;
                try {
                    if (gameBoard[myself.x][y] == 0) return true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ignore
                }
            }
        } else if ("E".equals(myself.direction)) {
            for (int i = 0; i <= ATTACH_RANGE; i++) {
                int x = myself.x + i;
                try {
                    if (gameBoard[x][myself.y] == 0) return true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ignore
                }
            }
        } else if ("S".equals(myself.direction)) {
            for (int i = 0; i <= ATTACH_RANGE; i++) {
                int y = myself.y + i;
                try {
                    if (gameBoard[myself.x][y] == 0) return true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ignore
                }
            }
        } else if ("W".equals(myself.direction)) {
            for (int i = 0; i <= ATTACH_RANGE; i++) {
                int x = myself.x - i;
                try {
                    if (gameBoard[x][myself.y] == 0) return true;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    public String getAction(PlayerState myself, PlayerState nextState) {
        if ("N".equals(myself.direction)) {
            if ("W".equals(nextState.direction)) {
                return "L";
            } else if ("E".equals(nextState.direction)) {
                return "R";
            }
        } else if ("E".equals(myself.direction)) {
            if ("N".equals(nextState.direction)) {
                return "L";
            } else if ("S".equals(nextState.direction)) {
                return "R";
            }
        } else if ("S".equals(myself.direction)) {
            if ("E".equals(nextState.direction)) {
                return "L";
            } else if ("W".equals(nextState.direction)) {
                return "R";
            }
        } else if ("W".equals(myself.direction)) {
            if ("S".equals(nextState.direction)) {
                return "L";
            } else if ("N".equals(nextState.direction)) {
                return "R";
            }
        }
        return "F";
    }

    public boolean between(int n, int a, int b) {
        if (a >= b) {
            return n <= a && n >= b;
        } else {
            return n <= b && n >= a;
        }
    }

    public static final int ATTACH_RANGE = 3;

    public List<PlayerState> possiblePlayerState(PlayerState currentState) {
        List<PlayerState> possibleStates = new LinkedList<>();

        String directions[];
        if ("N".equals(currentState.direction) || "S".equals(currentState.direction)) {
            directions = new String[] { "E", "W" };
        } else {
            directions = new String[] { "N", "S" };
        }
        for (String direction : directions) {
            PlayerState state = new PlayerState();
            state.direction = direction;
            state.x = currentState.x;
            state.y = currentState.y;
            possibleStates.add(state);
        }

        PlayerState state = new PlayerState();
        state.direction = currentState.direction;
        switch (state.direction) {
            case "N":
                state.x = currentState.x;
                state.y = currentState.y - 1;
                break;
            case "E":
                state.x = currentState.x + 1;
                state.y = currentState.y;
                break;
            case "S":
                state.x = currentState.x;
                state.y = currentState.y + 1;
                break;
            case "W":
                state.x = currentState.x - 1;
                state.y = currentState.y;
                break;
        }
        possibleStates.add(state);

        return possibleStates;
    }

    public boolean in0DeadZone(PlayerState attacker, int x, int y) {
        return x == attacker.x && y == attacker.y;
    }

    public boolean in1DeadZone(PlayerState attacker, int x, int y) {
        switch (attacker.direction) {
            case "N": return x == attacker.x && between(y, attacker.y, attacker.y - ATTACH_RANGE);
            case "E": return between(x, attacker.x, attacker.x + ATTACH_RANGE) && y == attacker.y;
            case "S": return x == attacker.x && between(y, attacker.y, attacker.y + ATTACH_RANGE);
            case "W": return between(x, attacker.x, attacker.x - ATTACH_RANGE) && y == attacker.y;
        }
        return false;
    }

    public boolean in2DeadZone(PlayerState attacker, int x, int y) {
        List<PlayerState> possibleStates1 = possiblePlayerState(attacker);
        for (PlayerState nextAttacker : possibleStates1) {
            if (in1DeadZone(nextAttacker, x, y)) return true;
        }
        return false;
    }

    public boolean in3DeadZone(PlayerState attacker, int x, int y) {
        List<PlayerState> possibleStates1 = possiblePlayerState(attacker);
        List<PlayerState> possibleStates2 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates1) {
            possibleStates2.addAll(possiblePlayerState(nextAttacker));
        }
        for (PlayerState nextAttacker : possibleStates2) {
            if (in1DeadZone(nextAttacker, x, y)) return true;
        }
        return false;
    }

    public boolean in4DeadZone(PlayerState attacker, int x, int y) {
        List<PlayerState> possibleStates1 = possiblePlayerState(attacker);
        List<PlayerState> possibleStates2 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates1) {
            possibleStates2.addAll(possiblePlayerState(nextAttacker));
        }
        List<PlayerState> possibleStates3 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates2) {
            possibleStates3.addAll(possiblePlayerState(nextAttacker));
        }
        for (PlayerState nextAttacker : possibleStates3) {
            if (in1DeadZone(nextAttacker, x, y)) return true;
        }
        return false;
    }

    public boolean in5DeadZone(PlayerState attacker, int x, int y) {
        List<PlayerState> possibleStates1 = possiblePlayerState(attacker);
        List<PlayerState> possibleStates2 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates1) {
            possibleStates2.addAll(possiblePlayerState(nextAttacker));
        }
        List<PlayerState> possibleStates3 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates2) {
            possibleStates3.addAll(possiblePlayerState(nextAttacker));
        }
        List<PlayerState> possibleStates4 = new LinkedList<>();
        for (PlayerState nextAttacker : possibleStates3) {
            possibleStates4.addAll(possiblePlayerState(nextAttacker));
        }
        for (PlayerState nextAttacker : possibleStates4) {
            if (in1DeadZone(nextAttacker, x, y)) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        ArenaUpdate arenaUpdate = new ArenaUpdate();
        arenaUpdate._links = new Links();
        arenaUpdate._links.self = new Self();
        arenaUpdate._links.self.href = "MYSELF";
        arenaUpdate.arena = new Arena();
        arenaUpdate.arena.dims = new ArrayList<>(2);
        arenaUpdate.arena.dims.add(0, 20);
        arenaUpdate.arena.dims.add(1, 10);
        arenaUpdate.arena.state = new HashMap<>();
        {
            PlayerState attacker = new PlayerState();
            attacker.direction = "E";
            attacker.x = 3;
            attacker.y = 5;
            arenaUpdate.arena.state.put("A", attacker);
        }
        {
            PlayerState attacker = new PlayerState();
            attacker.direction = "N";
            attacker.x = 5;
            attacker.y = 8;
            arenaUpdate.arena.state.put("B", attacker);
        }
        {
            PlayerState attacker = new PlayerState();
            attacker.direction = "N";
            attacker.x = 0;
            attacker.y = 5;
            arenaUpdate.arena.state.put("MYSELF", attacker);
        }

        Application app = new Application();
        app.index(arenaUpdate);
    }

    public int[][] calculateGameBoard(ArenaUpdate arenaUpdate) {
        int width = arenaUpdate.arena.dims.get(0);
        int height = arenaUpdate.arena.dims.get(1);
        int[][] gameBoard = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gameBoard[x][y] = -1;
            }
        }

        String myUrl = arenaUpdate._links.self.href;

        for (String key : arenaUpdate.arena.state.keySet()) {
            if (key.equals(myUrl)) {
                PlayerState myself = arenaUpdate.arena.state.get(myUrl);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (in0DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 1) {
                                gameBoard[x][y] += 6;
                            }
                        } else if (in1DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 1) {
                                gameBoard[x][y] += 5;
                            }
                        } else if (in2DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 2) {
                                gameBoard[x][y] += 4;
                            }
                        } else if (in3DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 3) {
                                gameBoard[x][y] += 3;
                            }
                        } else if (in4DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 4) {
                                gameBoard[x][y] += 2;
                            }
                        } else if (in5DeadZone(myself, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 5) {
                                gameBoard[x][y] += 1;
                            }
                        }
                    }
                }
            } else {
                PlayerState attacker = arenaUpdate.arena.state.get(key);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        if (in0DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 0) {
                                gameBoard[x][y] = 0;
                            }
                        } else if (in1DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 1) {
                                gameBoard[x][y] = 1;
                            }
                        } else if (in2DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 2) {
                                gameBoard[x][y] = 2;
                            }
                        } else if (in3DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 3) {
                                gameBoard[x][y] = 3;
                            }
                        } else if (in4DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 4) {
                                gameBoard[x][y] = 4;
                            }
                        } else if (in5DeadZone(attacker, x, y)) {
                            if (gameBoard[x][y] < 0 || gameBoard[x][y] > 5) {
                                gameBoard[x][y] = 5;
                            }
                        }
                    }
                }
            }
        }
        return gameBoard;
    }

    public void printGameBoard(ArenaUpdate arenaUpdate, int[][] gameBoard) {
        String myUrl = arenaUpdate._links.self.href;
        PlayerState myself = arenaUpdate.arena.state.get(myUrl);

        StringBuilder sb = new StringBuilder();

        int width = arenaUpdate.arena.dims.get(0);
        int height = arenaUpdate.arena.dims.get(1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == myself.x && y == myself.y) {
                    sb.append("X ");
                } else if (gameBoard[x][y] < 0) {
                    sb.append(". ");
                } else {
                    sb.append(gameBoard[x][y] + " ");
                }
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static class Self {
        public String href;
    }

    static class Links {
        public Self self;
    }

    static class PlayerState {
        public Integer x;
        public Integer y;
        public String direction;
        public Boolean wasHit;
        public Integer score;

        @Override
        public String toString() {
            return direction + " (" + x + ", " + y + ")";
        }
    }

    static class Arena {
        public List<Integer> dims;
        public Map<String, PlayerState> state;
    }

    static class ArenaUpdate {
        public Links _links;
        public Arena arena;
    }

}

