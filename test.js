var util = require('util');

var scalachess = require('./target/scala-2.11/scalachessjs-fastopt.js');

console.info("Start");

console.info("Scalachess:" + scalachess);

var pgn = '[Event "Fischer - Spassky World Championship Match"]\n' +
'[Site "Reykjavik ISL"]\n' +
'[Date "1972.08.22"]\n' +
'[EventDate "?"]\n' +
'[Round "17"]\n' +
'[Result "1/2-1/2"]\n' +
'[White "Boris Spassky"]\n' +
'[Black "Robert James Fischer"]\n' +
'[ECO "B09"]\n' +
'[WhiteElo "?"]\n' +
'[BlackElo "?"]\n' +
'[PlyCount "89"]\n' +
'\n' +
'1. e4 d6 2. d4 g6 3. Nc3 Nf6 4. f4 Bg7 5. Nf3 c5 6. dxc5 Qa5\n' +
'7. Bd3 Qxc5 8. Qe2 O-O 9. Be3 Qa5 10. O-O Bg4 11. Rad1 Nc6\n' +
'12. Bc4 Nh5 13. Bb3 Bxc3 14. bxc3 Qxc3 15. f5 Nf6 16. h3 Bxf3\n' +
'17. Qxf3 Na5 18. Rd3 Qc7 19. Bh6 Nxb3 20. cxb3 Qc5+ 21. Kh1\n' +
'Qe5 22. Bxf8 Rxf8 23. Re3 Rc8 24. fxg6 hxg6 25. Qf4 Qxf4\n' +
'26. Rxf4 Nd7 27. Rf2 Ne5 28. Kh2 Rc1 29. Ree2 Nc6 30. Rc2 Re1\n' +
'31. Rfe2 Ra1 32. Kg3 Kg7 33. Rcd2 Rf1 34. Rf2 Re1 35. Rfe2 Rf1\n' +
'36. Re3 a6 37. Rc3 Re1 38. Rc4 Rf1 39. Rdc2 Ra1 40. Rf2 Re1\n' +
'41. Rfc2 g5 42. Rc1 Re2 43. R1c2 Re1 44. Rc1 Re2 45. R1c2\n' +
'1/2-1/2';

var move = {
    fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2 +0+0',
    variant: 'threeCheck',
    pgnMoves: ['e4', 'e5'],
    uciMoves: ['e2e4', 'e7e5'],
    orig: 'd2',
    dest: 'd4',
    path: '0'
};

var threefold = {
    initialFen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    variant: 'standard',
    pgnMoves: ["e4", "d6", "d4", "g6", "Nc3", "Nf6", "f4", "Bg7", "Nf3", "c5", "dxc5", "Qa5", "Bd3", "Qxc5", "Qe2", "O-O", "Be3", "Qa5", "O-O", "Bg4", "Rad1", "Nc6", "Bc4", "Nh5", "Bb3", "Bxc3", "bxc3", "Qxc3", "f5", "Nf6", "h3", "Bxf3", "Qxf3", "Na5", "Rd3", "Qc7", "Bh6", "Nxb3", "cxb3", "Qc5+", "Kh1", "Qe5", "Bxf8", "Rxf8", "Re3", "Rc8", "fxg6", "hxg6", "Qf4", "Qxf4", "Rxf4", "Nd7", "Rf2", "Ne5", "Kh2", "Rc1", "Ree2", "Nc6", "Rc2", "Re1", "Rfe2", "Ra1", "Kg3", "Kg7", "Rcd2", "Rf1", "Rf2", "Re1", "Rfe2", "Rf1", "Re3", "a6", "Rc3", "Re1", "Rc4", "Rf1", "Rdc2", "Ra1", "Rf2", "Re1", "Rfc2", "g5", "Rc1", "Re2", "R1c2", "Re1", "Rc1", "Re2", "R1c2", "Re1", "Rc1"]
};

try {
    scalachessjs.Main().main();

    console.time('move');
    var res = scalachessjs.Main().move(move);
    //console.info("Move:" + util.inspect(res, { showHidden: true, depth: null }));
    console.timeEnd('move');

    console.time('getDests');
    res = scalachessjs.Main().getDests({
        fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
        variant: 'kingOfTheHill'
    });
    console.timeEnd('getDests');
    //console.info("Dests:" + util.inspect(res, { showHidden: true, depth: null }));

    console.time('threeFoldTest');
    res = scalachessjs.Main().threefoldTest(threefold);
    console.timeEnd('threeFoldTest');
    // console.info("3fold:" + util.inspect(res, { showHidden: true, depth: null }));

    console.time('pngRead');
    res = scalachessjs.Main().pgnRead({pgn: pgn});
    console.timeEnd('pngRead');
    //console.info("PGN:" + util.inspect(res, { showHidden: true, depth: null }));

    console.time('pngDump');
    res = scalachessjs.Main().pgnDump(threefold);
    console.timeEnd('pngDump');
    //console.info("PGN dump:" + util.inspect(res, { showHidden: true, depth: null }));
} catch(err) {
    console.info("Got error: " + err);
}

console.info("Done");
