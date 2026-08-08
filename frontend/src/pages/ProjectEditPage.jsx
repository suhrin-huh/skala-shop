import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { FiLock } from 'react-icons/fi';
import api from '../api/client';
import ProjectForm from '../components/ProjectForm';
import EmptyState from '../components/EmptyState';
import NotFoundPage from './NotFoundPage';
import Skeleton from '../components/Skeleton';
import { useAuth } from '../contexts/AuthContext';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './ProjectCreatePage.css';

/** 마감된 프로젝트는 수정할 수 없다. (domain-rules 8) */
const CLOSED_STATUSES = ['SUCCESS', 'FAILED'];

export default function ProjectEditPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');

  useEffect(() => {
    let cancelled = false;

    const fetchProject = async () => {
      setLoading(true);
      setNotFound(false);
      try {
        const res = await api.get(`/api/projects/${id}`);
        if (!cancelled) setProject(res.data.data);
      } catch (err) {
        if (cancelled) return;
        if (err.response?.status === 404) {
          setNotFound(true);
        } else {
          toast.error(extractApiMessage(err, '프로젝트를 불러오지 못했습니다.'));
          setNotFound(true);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    fetchProject();
    return () => {
      cancelled = true;
    };
  }, [id, toast]);

  // 프로젝트가 도착하면 폼 프리필 값으로 변환한다. 폼이 매 렌더 초기화되지 않도록 memo.
  const initialValues = useMemo(() => {
    if (!project) return null;
    return {
      title: project.title ?? '',
      categoryId: project.category?.id ?? '',
      description: project.description ?? '',
      mainImage: project.mainImage ?? '',
      targetAmount: project.targetAmount ?? 100000,
      startDate: project.startDate ?? '',
      endDate: project.endDate ?? '',
    };
  }, [project]);

  const handleSubmit = async (payload) => {
    setServerError('');
    setSubmitting(true);
    try {
      await api.put(`/api/projects/${id}`, payload);
      toast.success('프로젝트가 수정되었습니다.');
      navigate(`/projects/${id}`);
    } catch (err) {
      const message = extractApiMessage(err, '수정에 실패했습니다.');
      setServerError(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="project-form-page container">
        <div className="project-form-page-header">
          <Skeleton height={40} width={220} style={{ margin: '0 auto' }} />
        </div>
        <Skeleton height={560} radius="var(--rounded-md)" />
      </div>
    );
  }

  if (notFound || !project) {
    return (
      <NotFoundPage
        title="수정할 프로젝트를 찾을 수 없습니다"
        description="이미 삭제되었거나 존재하지 않는 프로젝트입니다."
      />
    );
  }

  // 창작자 본인이 아니면 폼 자체를 렌더링하지 않는다. (서버도 PROJECT_002 로 막지만 화면에서 먼저 차단)
  if (!user || user.id !== project.creator?.id) {
    return (
      <div className="project-form-page container">
        <EmptyState
          icon={FiLock}
          title="이 프로젝트를 수정할 권한이 없습니다"
          description="프로젝트는 등록한 창작자 본인만 수정할 수 있습니다."
          action={
            <Link to={`/projects/${id}`} className="btn-outline-pill">
              프로젝트 상세로 가기
            </Link>
          }
        />
      </div>
    );
  }

  if (CLOSED_STATUSES.includes(project.status)) {
    return (
      <div className="project-form-page container">
        <EmptyState
          icon={FiLock}
          title="이미 마감된 프로젝트입니다"
          description="펀딩이 종료된 프로젝트는 수정할 수 없습니다."
          action={
            <Link to={`/projects/${id}`} className="btn-outline-pill">
              프로젝트 상세로 가기
            </Link>
          }
        />
      </div>
    );
  }

  const pledgeCount = project.pledgeCount || 0;
  const warning =
    pledgeCount > 0
      ? `이미 ${pledgeCount.toLocaleString()}명이 후원한 프로젝트입니다. ` +
        '목표 금액이나 마감일을 바꾸면 이미 후원한 분들의 결제 조건이 달라지므로 변경 이력이 기록됩니다.'
      : null;

  return (
    <div className="project-form-page container">
      <div className="project-form-page-header">
        <h2 className="project-form-page-title">프로젝트 수정</h2>
        <p className="project-form-page-sub">{project.title}</p>
      </div>

      <ProjectForm
        initialValues={initialValues}
        onSubmit={handleSubmit}
        submitting={submitting}
        serverError={serverError}
        submitLabel="수정 내용 저장하기"
        submittingLabel="저장 중..."
        warning={warning}
      />
    </div>
  );
}
