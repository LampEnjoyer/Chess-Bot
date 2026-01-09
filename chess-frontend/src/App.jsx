import { useEffect, useState } from "react";
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import Square from './Square.jsx'
import ChessBoard from './ChessBoard.jsx'


function App() {
  const [message, setMessage] = useState("Loading...");


  useEffect(() => {
    fetch("http://localhost:8080/game/ping")
      .then(res => res.text()) //Response
      .then(data => setMessage(data)) //Body data
      .catch(err => {
        console.error(err);
        setMessage("Backend not reachable");
      });
  }, []);

  return ( <>
    <h1>{message}</h1>
    <ChessBoard />
  </>
    );
}






export default App;

