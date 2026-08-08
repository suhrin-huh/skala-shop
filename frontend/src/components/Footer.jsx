import React from "react";
import "./Footer.css";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-content">
        <div className="legal-band">
          <p>© SKALA-FUND. All rights reserved.</p>
          <p className="legal-notice">
            SKALA-FUND는 통신판매중개자이며 통신판매의 당사자가 아닙니다.
            프로젝트 완수 및 리워드 이행의 책임은 창작자에게 있습니다.
          </p>
        </div>
      </div>
    </footer>
  );
}
