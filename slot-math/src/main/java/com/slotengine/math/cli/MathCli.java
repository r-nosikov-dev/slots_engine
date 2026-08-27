package com.slotengine.math.cli;

import com.slotengine.engine.catalog.GameCatalog;
import com.slotengine.math.BaseGameEnumerator;
import com.slotengine.math.MonteCarloSimulator;
import com.slotengine.math.SimulationConfig;
import com.slotengine.model.GameDefinition;
import com.slotengine.model.PlayMode;

/**
 * Offline math workbench:
 * {@code java -jar slot-math.jar simulate golden-lynx 200000 20}
 * {@code java -jar slot-math.jar enumerate classic-fruits 10}
 */
public final class MathCli {

    public static void main(String[] args) {
        if (args.length == 0 || "help".equals(args[0])) {
            System.out.println("""
                    Slot Engine math CLI
                      list
                      simulate <gameId> [spins=100000] [bet] [mode=NORMAL] [seed=1]
                      enumerate <gameId> [bet]
                    """);
            return;
        }
        GameCatalog catalog = GameCatalog.builtin();
        switch (args[0]) {
            case "list" -> catalog.all().forEach(g ->
                    System.out.printf("%s  %s  %s  targetRTP=%s  vol=%s%n",
                            g.id(), g.name(), g.grid(), g.math().targetRtp(), g.math().volatility()));
            case "simulate" -> simulate(catalog, args);
            case "enumerate" -> enumerate(catalog, args);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                System.exit(1);
            }
        }
    }

    private static void simulate(GameCatalog catalog, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("simulate <gameId>");
        }
        GameDefinition game = catalog.require(args[1]);
        long spins = args.length > 2 ? Long.parseLong(args[2]) : 100_000L;
        long bet = args.length > 3 ? Long.parseLong(args[3]) : defaultBet(game);
        PlayMode mode = args.length > 4 ? PlayMode.valueOf(args[4]) : PlayMode.NORMAL;
        long seed = args.length > 5 ? Long.parseLong(args[5]) : 1L;
        var report = new MonteCarloSimulator().simulate(game, new SimulationConfig(spins, bet, seed, mode, 0));
        report.asMap().forEach((k, v) -> System.out.println(k + "=" + v));
    }

    private static void enumerate(GameCatalog catalog, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("enumerate <gameId>");
        }
        GameDefinition game = catalog.require(args[1]);
        long bet = args.length > 2 ? Long.parseLong(args[2]) : defaultBet(game);
        var report = new BaseGameEnumerator().enumerate(game, bet);
        report.asMap().forEach((k, v) -> System.out.println(k + "=" + v));
    }

    private static long defaultBet(GameDefinition game) {
        if (game.evaluation().name().equals("PAYLINES") && game.lineCount() > 0) {
            return game.lineCount();
        }
        return 1;
    }
}
