declare module "react-native-http-bridge";

export declare function start(
  port: number,
  serviceName: string,
  callback: (request: Request) => void,
  wwwroot?: string,
  isEnabledForDeliveryFilesInParts?: boolean,
): void;

export declare interface Request {
  requestId: string;
  postData?: {};
  type: string;
  url: string;
  params: string;
}

export declare function stop(): void;

export declare function respond(
  requestId: string,
  code: number,
  type: string,
  body: string
): void;