export interface ExtractedRecord {
  id: string; // Manifest / Load #
  vehicle_details: string;
  vin: string;
  origin: string;
  destination: string;
  for_client: string;
  hand_written: string;
  price: number;
  drivetrain: string;
  epb: string; // "Yes" | "No"
}

export interface DBEntry extends ExtractedRecord {
  date_hauled: string;
  extracted_at: string;
  id_db?: string;
}

export interface AuditLog {
  timestamp: string;
  action: string;
  details: string;
}

export interface DBState {
  entries: DBEntry[];
  logs: AuditLog[];
}
