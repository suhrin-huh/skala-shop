import { useCallback, useEffect, useState } from 'react';
import api from '../api/client';

const STORAGE_KEY = 'skala-fund:recently-viewed';
const MAX_ITEMS = 10;

/**
 * 최근 본 펀딩.
 *
 * LocalStorage 에는 **ID 만** 저장한다. 제목·모금액 같은 값을 캐시하면
 * 다시 볼 때 옛날 숫자가 보이므로, 조회는 항상 GET /api/projects?ids= 로 최신값을 받는다.
 *
 * 큐 규칙: 최대 10개 FIFO. 이미 있는 ID 로 다시 들어오면 기존 항목을 제거하고 맨 앞에 다시 넣는다.
 * 서버는 삭제된 프로젝트 ID 를 404 대신 조용히 빼고 응답하므로,
 * 응답에 없는 ID 는 LocalStorage 에서도 정리한다.
 */

function readIds() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((v) => Number(v))
      .filter((v) => Number.isInteger(v) && v > 0)
      .slice(0, MAX_ITEMS);
  } catch {
    // JSON 이 깨졌거나 스토리지 접근이 막힌 환경(프라이빗 모드 등)
    return [];
  }
}

function writeIds(ids) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids.slice(0, MAX_ITEMS)));
  } catch {
    // 저장 실패는 조용히 무시한다. 최근 본 항목 때문에 화면이 깨지면 안 된다.
  }
}

/** 상세 진입 시 호출. 중복이면 제거 후 맨 앞 재삽입, 넘치면 뒤에서 잘라낸다. */
export function recordRecentlyViewed(projectId) {
  const id = Number(projectId);
  if (!Number.isInteger(id) || id <= 0) return;

  const next = [id, ...readIds().filter((v) => v !== id)].slice(0, MAX_ITEMS);
  writeIds(next);
}

export function getRecentlyViewedIds() {
  return readIds();
}

export function clearRecentlyViewed() {
  writeIds([]);
}

/**
 * 저장된 ID 목록으로 최신 프로젝트 정보를 재조회한다.
 * 반환 순서는 저장 순서(최근 본 순)를 유지한다.
 */
export default function useRecentlyViewed({ enabled = true } = {}) {
  const [projects, setProjects] = useState([]);
  // enabled 가 false 인 동안에도 true 로 둔다. 탭을 켜는 순간 빈 상태가 한 프레임 스치는 것을 막는다.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    const ids = readIds();
    if (ids.length === 0) {
      setProjects([]);
      setError(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      // ?ids= 응답은 Page 가 아니라 배열이다.
      const res = await api.get('/api/projects', { params: { ids: ids.join(',') } });
      const fetched = res.data.data || [];

      const byId = new Map(fetched.map((p) => [p.id, p]));
      const survivingIds = ids.filter((id) => byId.has(id));

      // 서버가 조용히 걸러낸 ID(삭제됨)는 LocalStorage 에서도 지운다.
      if (survivingIds.length !== ids.length) {
        writeIds(survivingIds);
      }

      setProjects(survivingIds.map((id) => byId.get(id)));
      setError(null);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;
    load();
  }, [enabled, load]);

  const clear = useCallback(() => {
    clearRecentlyViewed();
    setProjects([]);
  }, []);

  return { projects, loading, error, reload: load, clear, record: recordRecentlyViewed };
}
