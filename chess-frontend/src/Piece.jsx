import { pieces } from './images/index.js'
import './Piece.css'

function Piece( {piece, row, col, onClick}){
    if(!piece){
        return null; //Empty Square
    }

    const color = piece[0] === "w" ? "white" : "black";
    const type = piece[1];
    const typeMap = {
        p: "pawn",
        r: "rook",
        n: "knight",
        b: "bishop",
        q: "queen",
        k: "king",
      };
    const imgSrc = pieces[`${color}_${typeMap[type]}`];

    const handleClick = () => {
        console.log(imgSrc);
    };

    const handleDrag = (e) =>{
        e.dataTransfer.setData(
            "text/plain",
            JSON.stringify({ row, col })
        );
        console.log("drag start");
    };

    return <img
            src={imgSrc}
            alt={`${color}_${typeMap[type]}`}
            className="piece"
            onClick = {handleClick}
            draggable = {true}
            onDragStart = {handleDrag}
        />;
}

export default Piece;