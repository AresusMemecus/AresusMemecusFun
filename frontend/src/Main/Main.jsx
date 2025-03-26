import React from "react";

function Main() {
    return (
        <div className="main">
            <div className="main-page">
            <div className="main-content">
                <div className="main-text">
                    <p>личный ЩитПостный сайт</p>
                </div>
                <div className="main-img">
                    <img src="https://i.imgur.com/GNdpyns.png"></img>
                </div>
            </div>
            <div className="main-app">
                <div className="main-app-button" onClick={() => (window.location.href= "/app")}>Прилажухи</div>
                <div className="main-app-button tg" onClick={() => window.open("https://t.me/AresusMemecus", "_blank")}>Телеграм</div>
            </div>
        </div>
        </div>
    )
}

export default Main;