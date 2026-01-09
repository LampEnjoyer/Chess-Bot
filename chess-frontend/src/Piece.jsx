import { pieces } from './images/index.js'
import './Piece.css'

function Piece( {piece}){
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

    return <img src={imgSrc} alt={`${color}_${typeMap[type]}`} className="piece" />;
}

export default Piece;