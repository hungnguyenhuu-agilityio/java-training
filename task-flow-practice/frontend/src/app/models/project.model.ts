export interface Project {
  id: number;
  name: string;
  description?: string;
}

export interface ProjectCreateRequest {
  name: string;
  description?: string;
}
