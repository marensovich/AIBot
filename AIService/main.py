from openai import OpenAI

from fastapi import FastAPI
from pydantic import BaseModel


client = OpenAI(api_key="APIKEY", base_url="https://api.deepseek.com")


app = FastAPI()

class RequestData(BaseModel):
    text: str


@app.post("/aks_ai")
def ask_ai(request: RequestData):
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {"role": "system", "content": "You are a helpful assistant"},
            {"role": "user", "content": request},
        ],
        stream=False
    )
    print(response.choices[0].message.content)
    return {"responce":response.choices[0].message.content}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9090)


