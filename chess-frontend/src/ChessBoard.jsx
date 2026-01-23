import Square from './Square.jsx'
import Piece from './Piece.jsx'
import './ChessBoard.css'
import { useEffect, useState } from "react";
import { pieces } from './images/index.js'
import GameControls from './GameControls.jsx'

const initialBoard = [
  ["br","bn","bb","bq","bk","bb","bn","br"],
  ["bp","bp","bp","bp","bp","bp","bp","bp"],
  [null,null,null,null,null,null,null,null],
  [null,null,null,null,null,null,null,null],
  [null,null,null,null,null,null,null,null],
  [null,null,null,null,null,null,null,null],
  ["wp","wp","wp","wp","wp","wp","wp","wp"],
  ["wr","wn","wb","wq","wk","wb","wn","wr"],
];

function ChessBoard(){
    const [board, setBoard] = useState(initialBoard);

    const playerMove = (fromRow, fromCol, toRow, toCol) => {

        const uciMove = getMoveUCI(fromRow, fromCol, toRow, toCol)
        return fetch("http://localhost:8080/game/move", {
            method: "POST",
            headers:{ "Content-Type" : "application/json"},
            body: JSON.stringify(uciMove)
        })
        .then(res => {
            if(res.ok){ //Good Request
                return res.json();
            }else{
                throw new Error('Invalid move');
            }
        })
        .then(data => {
            console.log(`Move from Row: ${fromRow}, Col: ${fromCol} to Row: ${toRow} Col: ${toCol}`);
            setBoard(fenToBoard(data.fen));
        })
        .catch(err => console.error(err));
    }

    const resetGame = () => {
        console.log("Resetting game");
        return fetch("http://localhost:8080/game/new", {
            method: "POST",
        })
        .then(res => {
            if(res.ok){
                return res.json();
            }else{
                throw new Error('Invalid move');
            }
        })
        .then(data => {
            console.log("Updated game state:", data.fen);
            setBoard(fenToBoard(data.fen));
        })
    }

    const aiMove = () => {
        fetch("http://localhost:8080/game/ai-move", {
            method: "POST",
        })
        .then(res => {
           if(res.ok){
               return res.json();
           }else{
               throw new Error('Invalid move');
           }
        })
        .then(data => {
            if(data.checkMate){
                alert("Checkmate!");
                return;
            }
            if (data.inCheck) {
                    console.log("King is in check");
            }
            console.log("Moved");
            setBoard(fenToBoard(data.fen));
        })
    }

    const handleMove = async (fromRow, fromCol, toRow, toCol) =>{
        try {
                await playerMove(fromRow, fromCol, toRow, toCol);
                await aiMove();
            } catch (err) {
                console.error("Move failed:", err);
            }
    };

    return (
      <div>
          <GameControls onReset = {resetGame} />
          <div className="board">
            {[7,6,5,4,3,2,1,0].map(row =>
              [0,1,2,3,4,5,6,7].map(col => {
                const pieceData = board[7 - row][col];
                return (
                  <Square key={`${row}-${col}`} row={row} col={col} onMove = {handleMove}>
                    {pieceData && <Piece piece={pieceData} row={row} col={col} />}
                  </Square>
                );
              })
            )}
          </div>
      </div>
    );
}

function getMoveUCI(fromRow, fromCol, toRow, toCol){
    const fromFile = String.fromCharCode(97 + fromCol); // 97 is 'a'
    const fromRank = fromRow + 1;
    const toFile = String.fromCharCode(97 + toCol);
    const toRank = toRow + 1;
    const move = `${fromFile}${fromRank}${toFile}${toRank}`
    console.log(move);
    return move;
}

function fenToBoard(fen){
    const rows = fen.split('/');
    const boards = [];
    for(let row of rows){
        const boardRow = [];
        for(let char of row){
            if(isNaN(char)){ //White piece
                if(char === char.toUpperCase()){
                    boardRow.push("w" + char.toLowerCase());
                }else{
                    boardRow.push("b" + char);
                }
            }else{
                for(let i = 0; i<parseInt(char); i++){
                    boardRow.push(null);
                }
            }
        }
        boards.push(boardRow);
    }
    return boards;
}

export default ChessBoard;