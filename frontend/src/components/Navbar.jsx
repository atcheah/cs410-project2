import React from 'react'
import graph_legend from '../assets/graph_legend.svg'

// code referenced from https://stackoverflow.com/questions/29884654/button-that-refreshes-the-page-on-click

const Navbar = () => {
    return (
        <div className="navbar bg-primary text-primary-content">
            <button className="btn btn-ghost text-xl" onClick={() => window.location.reload()}>Group 19's Control Flow Analysis Tool</button>
            <button className="btn btn-ghost" onClick={() => document.getElementById('my_modal_2').showModal()}>Graph Legend</button>
            <dialog id="my_modal_2" className="modal">
                <div className="modal-box w-100%">
                    <h3 className="font-bold text-lg pb-5">Graph Legend</h3>
                    <img src={graph_legend} alt="Legend" />
                </div>
                <form method="dialog" class="modal-backdrop">
                    <button>close</button>
                </form>
            </dialog>
        </div>
    )
}

export default Navbar