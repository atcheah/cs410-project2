// import graph from './assets/graph.png';
import './App.css';
import Navbar from "./components/Navbar";
import { Theme } from "daisyui";
import React, { useEffect, useState } from "react";
import axios from 'axios';
import CodeMirror from "@uiw/react-codemirror";
import { java } from '@codemirror/lang-java';
import { githubLight } from '@uiw/codemirror-theme-github';

function App() {
    const theme = "nord"; // as Theme;

    const [currentState, setCurrentState] = useState("uninitialized"); // [graph, setGraph
    const [inputCode, setInputCode] = useState("");
    const [graph, setGraph] = useState(null);

    useEffect(() => {
        const sendTextToBackend = async () => {
            setCurrentState("loading");
            // code referenced from https://stackoverflow.com/questions/69400766/get-image-through-axios-how-to-display-on-my-react-project
            axios.post('/graph', { inputCode }, { headers: {'Content-Type': 'application/json'}, baseURL: "http://localhost:8080", responseType: "arraybuffer" })
                .then(response => {
                    // console.log("response: " + response.data);
                    // code referenced from https://stackoverflow.com/questions/42785229/axios-serving-png-image-is-giving-broken-image
                    let blob = new Blob([response.data], {type: response.headers['content-type']})
                    let image = URL.createObjectURL(blob);
                    setGraph(image);
                    setCurrentState("graph");

                })
                .catch(error => {
                    setCurrentState("error");
                });
        };
        if (inputCode !== "") {
            sendTextToBackend();
        } else {
            setCurrentState("uninitialized");
        }
    }, [inputCode]);

    const handleTextInputChange = (value) => {
        setInputCode(value);
    };

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
    }, [theme]);

    return (
        <div className="App">
            <Navbar></Navbar>
            <body>
            <div className="flex-auto justify-evenly p-5">
                <div className="card bg-primary text-primary-content justify-start mb-4">
                    <div className="card-body">
                        <h2 className="card-title">Welcome to Group 19's Control Flow Analysis Tool!</h2>
                        <p>Enter Java code into the text area below to generate a control flow graph</p>
                        <h3 className="card-title mt-2">Enter Your Code Here:</h3>
                        <CodeMirror
                            value={inputCode}
                            onChange={handleTextInputChange}
                            theme={githubLight}
                            extensions={[java()]}
                            placeholder="Enter Code here"
                        />
                    </div>
                </div>
                <div>
                    {currentState === "uninitialized" &&
                        <div className="card bg-primary text-primary-content justify-start">
                            <div className="card-body">
                                <h2 className="card-title">No Graph Yet...</h2>
                                <p>Enter Code To See a Graph</p>
                            </div>
                        </div>}
                    {currentState === "loading" &&
                        <div className="card bg-primary text-primary-content justify-start">
                            <div className="card-body">
                                <h2 className="card-title">Loading...</h2>
                                <p>Graph is generating, please wait.</p>
                            </div>
                        </div>}
                    {currentState === "graph" &&
                        <div className="card bg-primary text-primary-content justify-start">
                            <div className="card-body">
                                <div className="flex justify-between">
                                    <h2 className="card-title">Generated Graph:</h2>
                                    <a href={graph} download="graph.svg">
                                        <button className='btn'>Download</button>
                                    </a>
                                </div>
                                <img src={graph} alt="Graph"/>
                            </div>
                        </div>}
                    {currentState === "error" &&
                        <div className="card bg-primary text-primary-content justify-start">
                            <div className="card-body">
                                <h2 className="card-title">Error:</h2>
                                <p>There was an error processing your code. Please try again.</p>
                            </div>
                        </div>}
                </div>
            </div>
            </body>
        </div>
    );
}

export default App;
