import React from 'react';
import { Link } from 'react-router-dom';
import './NotFoundPage.css';

/**
 * 404 전용 페이지.
 *
 * 라우트 미스매치(path="*")뿐 아니라, 삭제된 프로젝트 상세(404 + PROJECT_001)에서도
 * 같은 화면을 쓴다. 이전처럼 조용히 "/" 로 리다이렉트하면 사용자는
 * 주소를 잘못 친 것인지 프로젝트가 사라진 것인지 알 수 없다.
 */
export default function NotFoundPage({
  code = '404',
  title = '페이지를 찾을 수 없습니다',
  description = '주소가 바뀌었거나 삭제된 페이지일 수 있습니다.',
  actions,
}) {
  return (
    <div className="not-found-page container">
      <p className="not-found-code">{code}</p>
      <h1 className="not-found-title">{title}</h1>
      <p className="not-found-description">{description}</p>

      <div className="not-found-actions">
        {actions || (
          <>
            <Link to="/" className="btn-primary">
              홈으로 가기
            </Link>
            <Link to="/projects" className="btn-outline-pill">
              프로젝트 둘러보기
            </Link>
          </>
        )}
      </div>
    </div>
  );
}
