import React, {useEffect} from "react";

function Apps(){

    useEffect(() => {
        document.title = "App's";
      }, []);

    return(
        <div className="app-main">
            <div className="app-item">
                <div className="app-cliper" onClick={() => (window.location.href ="/app/clips")}>Clipper</div>
            </div>
        </div>
    )
}

export default Apps;