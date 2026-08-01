export interface Load { rowNumber: number; vehicleDetails: string; vin: string; origin: string; destination: string; notes: string; drivetrain: string; epb: string; completed: boolean; }
export interface DailySheet { spreadsheetId: string; completedColumn: number; loads: Load[]; }
