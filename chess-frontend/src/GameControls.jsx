function GameControls( {onReset}){
    return (
      <div>
        <button type = "button" onClick = {onReset}>
            New Game
        </button>
      </div>
    );
}

export default GameControls;