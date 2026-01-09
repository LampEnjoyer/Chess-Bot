import Square from './Square.jsx'
import './ChessBoard.css'
import React from "react"

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
    return (
         <div className = "board">
             {[7,6,5,4,3,2,1,0].map(row =>
                 [0,1,2,3,4,5,6,7].map(col =>
                     <Square key={`${row}-${col}`} row={row} col={col} piece = {initialBoard[7-row][col]} />
                 )
             )}
         </div>
    );
}

export default ChessBoard;