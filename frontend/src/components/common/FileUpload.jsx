import React, { useCallback } from 'react';
import { UploadCloud, X, FileText } from 'lucide-react';

export const FileUpload = ({
  onFilesSelected,
  acceptedTypes = '*',
  multiple = false,
  label = 'Upload File',
  selectedFiles = []
}) => {
  const handleDragOver = useCallback((e) => {
    e.preventDefault();
  }, []);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      onFilesSelected(multiple ? files : [files[0]]);
    }
  }, [multiple, onFilesSelected]);

  const handleChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files);
      onFilesSelected(multiple ? files : [files[0]]);
    }
  };

  const removeFile = (indexToRemove) => {
    const newFiles = selectedFiles.filter((_, idx) => idx !== indexToRemove);
    onFilesSelected(newFiles);
  };

  return (
    <div className="w-full">
      {label && <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>}
      <div 
        className="w-full border-2 border-dashed border-gray-300 rounded-lg p-6 flex flex-col items-center justify-center text-center hover:bg-gray-50 transition-colors cursor-pointer bg-white"
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onClick={() => document.getElementById(`file-upload-${label}`).click()}
      >
        <UploadCloud className="w-10 h-10 text-gray-400 mb-3" />
        <p className="text-sm font-medium text-gray-700">Drag files here or click to browse</p>
        <p className="text-xs text-gray-500 mt-1">Maximum file size: 5MB</p>
        <input 
          id={`file-upload-${label}`}
          type="file" 
          className="hidden" 
          accept={acceptedTypes}
          multiple={multiple}
          onChange={handleChange}
        />
      </div>

      {selectedFiles.length > 0 && (
        <div className="mt-4 space-y-2">
          {selectedFiles.map((file, idx) => (
            <div key={idx} className="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg">
              <div className="flex items-center gap-3 overflow-hidden">
                <FileText className="text-accent-500 flex-shrink-0" size={20} />
                <span className="text-sm text-gray-700 truncate">{file.name}</span>
                <span className="text-xs text-gray-500 ml-2 flex-shrink-0">
                  {(file.size / 1024 / 1024).toFixed(2)} MB
                </span>
              </div>
              <button 
                type="button" 
                onClick={(e) => { e.stopPropagation(); removeFile(idx); }}
                className="text-gray-400 hover:text-red-500 p-1 rounded transition-colors"
              >
                <X size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
export default FileUpload;
