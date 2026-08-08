import React, { useEffect, useRef, useState } from 'react';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import api from '../api/client';
import { useToast, extractApiMessage } from '../contexts/ToastContext';
import './ImageCropModal.css';

const ASPECT = 16 / 10;

function centeredAspectCrop(mediaWidth, mediaHeight) {
  return centerCrop(
    makeAspectCrop({ unit: '%', width: 90 }, ASPECT, mediaWidth, mediaHeight),
    mediaWidth,
    mediaHeight
  );
}

/** completedCrop 은 화면에 렌더된 이미지 기준 픽셀 좌표라, 원본 해상도로 스케일을 맞춰 canvas 에 그린다. */
function cropToBlob(image, crop) {
  const scaleX = image.naturalWidth / image.width;
  const scaleY = image.naturalHeight / image.height;

  const canvas = document.createElement('canvas');
  canvas.width = Math.round(crop.width * scaleX);
  canvas.height = Math.round(crop.height * scaleY);

  const ctx = canvas.getContext('2d');
  ctx.drawImage(
    image,
    crop.x * scaleX,
    crop.y * scaleY,
    crop.width * scaleX,
    crop.height * scaleY,
    0,
    0,
    canvas.width,
    canvas.height
  );

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('이미지를 변환하지 못했습니다.'))),
      'image/jpeg',
      0.92
    );
  });
}

/**
 * 파일 선택 직후 16:10 크롭 UI를 띄우고, "적용" 시 즉시 잘라낸 이미지를
 * 기존 업로드 엔드포인트(POST /api/files/images)로 올려 URL을 부모에 돌려준다.
 */
export default function ImageCropModal({ imageSrc, onCancel, onUploaded }) {
  const imgRef = useRef(null);
  const [crop, setCrop] = useState();
  const [completedCrop, setCompletedCrop] = useState(null);
  const [uploading, setUploading] = useState(false);
  const toast = useToast();

  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === 'Escape' && !uploading) onCancel();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onCancel, uploading]);

  const handleImageLoad = (e) => {
    const { width, height } = e.currentTarget;
    const initial = centeredAspectCrop(width, height);
    setCrop(initial);
    setCompletedCrop(initial);
  };

  const handleCancel = () => {
    if (uploading) return;
    onCancel();
  };

  const handleApply = async () => {
    if (!completedCrop || !imgRef.current || completedCrop.width === 0) return;

    setUploading(true);
    try {
      const blob = await cropToBlob(imgRef.current, completedCrop);
      const formData = new FormData();
      formData.append('file', blob, 'main-image.jpg');
      const res = await api.post('/api/files/images', formData);
      onUploaded(res.data.data.url);
    } catch (err) {
      toast.error(extractApiMessage(err, '이미지 업로드에 실패했습니다.'));
      setUploading(false);
    }
  };

  return (
    <div className="modal-scrim" onClick={handleCancel}>
      <div
        className="modal-card image-crop-modal-card"
        role="dialog"
        aria-modal="true"
        aria-label="대표 이미지 자르기"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="image-crop-modal-title">대표 이미지 자르기</h3>
        <p className="image-crop-modal-desc">16:10 비율로 노출됩니다. 영역을 드래그해 조정하세요.</p>

        <div className="image-crop-modal-stage">
          <ReactCrop
            crop={crop}
            onChange={(_, percentCrop) => setCrop(percentCrop)}
            onComplete={(pixelCrop) => setCompletedCrop(pixelCrop)}
            aspect={ASPECT}
          >
            <img ref={imgRef} src={imageSrc} onLoad={handleImageLoad} alt="자르기 대상" />
          </ReactCrop>
        </div>

        <div className="image-crop-modal-actions">
          <button type="button" className="btn-cancel" onClick={handleCancel} disabled={uploading}>
            취소
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={handleApply}
            disabled={uploading || !completedCrop}
          >
            {uploading ? '업로드 중...' : '적용'}
          </button>
        </div>
      </div>
    </div>
  );
}
