import './Square.css'
import Piece from './Piece.jsx'
import { pieces } from './images/index.js'


function Square( {row, col, onMove, children} ){
    const isLight = (row + col) % 2 == 1;

    const handleClick = () => {
        console.log("Row:" + row + " Col: " + col);
    };

    const handleDragStart = (e) => {
            e.dataTransfer.setData(
                "text/plain",
                JSON.stringify({ row, col })
            );
            console.log("drag start from square:", row, col);
    };

    const handleDropOver = (e) => { //nothing matters here
        e.preventDefault();
    };

    const handleDrop = (e) => {
        e.preventDefault();
        const data = JSON.parse(e.dataTransfer.getData("text/plain"));
        console.log(`Moving from (${data.row}, ${data.col}) to (${row}, ${col})`);
         if (onMove) {
             onMove(data.row, data.col, row, col);
         }
    }

    return (
        <div
            onClick = {handleClick}
            className={`square ${isLight ? "light" : "dark"}`}
            onDragStart = {handleDragStart}
            onDragOver = {handleDropOver}
            onDrop = {handleDrop}
        >
         {children}

        </div>
    );
}


export default Square;