import React from 'react';
import './Footer.css';

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-content">
        <div className="footer-columns">
          <div className="footer-col">
            <h4 className="footer-col-title">텀블벅</h4>
            <ul>
              <li><a href="#">공지사항</a></li>
              <li><a href="#">서비스 소개</a></li>
              <li><a href="#">채용</a></li>
            </ul>
          </div>
          <div className="footer-col">
            <h4 className="footer-col-title">고객지원</h4>
            <ul>
              <li><a href="#">헬프 센터</a></li>
              <li><a href="#">문의하기</a></li>
              <li><a href="#">공지사항</a></li>
            </ul>
          </div>
          <div className="footer-col">
            <h4 className="footer-col-title">창작자</h4>
            <ul>
              <li><a href="#">창작자 가이드</a></li>
              <li><a href="#">프로젝트 대시보드</a></li>
              <li><a href="#">수수료 안내</a></li>
            </ul>
          </div>
          <div className="footer-col">
            <h4 className="footer-col-title">팔로우</h4>
            <ul>
              <li><a href="#">인스타그램</a></li>
              <li><a href="#">트위터</a></li>
              <li><a href="#">페이스북</a></li>
            </ul>
          </div>
        </div>

        <div className="legal-band">
          <p>© SKALA-FUND. All rights reserved.</p>
          <p className="legal-notice">
            텀블벅(SKALA-FUND)은 통신판매중개자이며 통신판매의 당사자가 아닙니다. 프로젝트 완수 및 리워드 이행의 책임은 창작자에게 있습니다.
          </p>
        </div>
      </div>
    </footer>
  );
}
