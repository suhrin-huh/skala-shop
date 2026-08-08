import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import ProjectForm from '../components/ProjectForm';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './ProjectCreatePage.css';

export default function ProjectCreatePage() {
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();
  const toast = useToast();

  const handleSubmit = async (payload) => {
    setServerError('');
    setSubmitting(true);
    try {
      const res = await api.post('/api/projects', payload);
      toast.success('프로젝트가 등록되었습니다.');
      navigate(`/projects/${res.data.data.id}`);
    } catch (err) {
      const message = extractApiMessage(err, '등록에 실패했습니다.');
      setServerError(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="project-form-page container">
      <div className="project-form-page-header">
        <h2 className="project-form-page-title">프로젝트 올리기</h2>
        <p className="project-form-page-sub">
          목표 금액을 채운 프로젝트만 마감일에 결제됩니다.
        </p>
      </div>

      <ProjectForm
        onSubmit={handleSubmit}
        submitting={submitting}
        serverError={serverError}
        submitLabel="프로젝트 등록하기"
        submittingLabel="등록 중..."
      />
    </div>
  );
}
