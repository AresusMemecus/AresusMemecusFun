import React from "react";

function Header() {

    return (
        <div className="header">
            <div class="logo-container" onClick={() => (window.location.href ="/")}>
                <img src="https://avatars.githubusercontent.com/u/126894184?s=400&amp;u=b1a4ce7d58fa5c7bfc9d64563ad7fba8e41b5f75&amp;v=4" alt="logo" class="logo"></img>
                Aresus Memecus
            </div>
        </div>
    );
}

export default Header