import './Square.css'
import Piece from './Piece.jsx'


function Square( {row, col, piece, onClick} ){
    const isLight = (row + col) % 2 == 1;

    const handleClick = () => {
        console.log("Row:" + row + " Col: " + col);
    };

    return (
        <div
            onClick = {handleClick}
            className={`square ${isLight ? "light" : "dark"}`}>
            <Piece piece = {piece} />
        </div>
    );
}


export default Square;